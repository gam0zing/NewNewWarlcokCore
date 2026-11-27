package com.gam0zing.newnew_warlock_core.client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.cyberday1.neoorigins.client.ClientPlayerPowers;
import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * 客户端玩家皮肤染色：读取同步到客户端的 {@code model_color:r:g:b:a} capability，
 * 生成带 alpha 的染色皮肤纹理，并替换 {@link PlayerSkin}。
 */
public final class ModelColorSkinTint {

    private static final String CAPABILITY_PREFIX = "model_color:";
    private static final String TEXTURE_PREFIX = "tinted_skin/";

    private static final Map<UUID, CachedSkin> CACHE = new HashMap<>();
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    /** {@return 染色后的皮肤；没有 model_color 能力时返回原皮肤} */
    public static PlayerSkin apply(UUID uuid, PlayerSkin original) {
        if (original == null || isTinted(original)) {
            return original;
        }
        String colorKey = findColorCapability(uuid);
        if (colorKey == null) {
            release(uuid);
            return original;
        }
        Identifier source = original.body().texturePath();
        CachedSkin cached;
        synchronized (CACHE) {
            cached = CACHE.get(uuid);
        }
        if (cached != null && cached.source().equals(source) && cached.colorKey().equals(colorKey)
            && Minecraft.getInstance().getTextureManager().getTexture(cached.tintedPath()) != null) {
            return cached.skin();
        }
        Tint tint = Tint.parse(colorKey);
        if (tint == null) {
            return original;
        }
        Identifier tintedPath = Identifier.fromNamespaceAndPath(
            NewNewWarlockCore.MOD_ID,
            TEXTURE_PREFIX + uuid.toString().replace("-", "") + "/" + SEQUENCE.incrementAndGet());
        PlayerSkin tinted = createTintedSkin(original, source, tintedPath, tint);
        if (tinted == null) {
            return original;
        }
        CachedSkin previous;
        synchronized (CACHE) {
            previous = CACHE.put(uuid, new CachedSkin(source, colorKey, tintedPath, tinted));
        }
        if (previous != null) {
            releaseTexture(previous.tintedPath());
        }
        return tinted;
    }

    /** {@return 该皮肤是否由本模组生成} */
    public static boolean isTinted(PlayerSkin skin) {
        if (skin == null) {
            return false;
        }
        Identifier path = skin.body().texturePath();
        return path.getNamespace().equals(NewNewWarlockCore.MOD_ID)
            && path.getPath().startsWith(TEXTURE_PREFIX);
    }

    private static String findColorCapability(UUID uuid) {
        for (String capability : ClientPlayerPowers.capabilities(uuid)) {
            if (capability.startsWith(CAPABILITY_PREFIX)) {
                return capability;
            }
        }
        return null;
    }

    private static PlayerSkin createTintedSkin(
        PlayerSkin original, Identifier source, Identifier tintedPath, Tint tint) {
        Minecraft minecraft = Minecraft.getInstance();
        TextureManager textures = minecraft.getTextureManager();
        NativeImage image = null;
        boolean closeImage = false;
        AbstractTexture texture = textures.getTexture(source);
        if (texture instanceof DynamicTexture dynamic) {
            image = dynamic.getPixels();
        }
        if (image == null) {
            Optional<Resource> resource = minecraft.getResourceManager().getResource(source);
            if (resource.isPresent()) {
                try (InputStream input = resource.get().open()) {
                    image = NativeImage.read(input);
                    closeImage = true;
                } catch (Exception ignored) {
                    image = null;
                }
            }
        }
        if (image == null) {
            return null;
        }
        NativeImage tintedImage;
        try {
            tintedImage = tintImage(image, tint);
        } finally {
            if (closeImage) {
                image.close();
            }
        }
        if (tintedImage == null) {
            return null;
        }
        try {
            DynamicTexture dynamicTexture =
                new DynamicTexture(() -> "newnew_warlock_core tinted skin", tintedImage);
            dynamicTexture.upload();
            textures.register(tintedPath, dynamicTexture);
        } catch (RuntimeException exception) {
            tintedImage.close();
            return null;
        }
        // 双参构造：texturePath 必须等于动态纹理注册 id，单参构造会自动补 textures/ 前缀和 .png。
        ClientAsset.Texture body = new ClientAsset.ResourceTexture(tintedPath, tintedPath);
        return new PlayerSkin(body, original.cape(), original.elytra(), original.model(), original.secure());
    }

    private static NativeImage tintImage(NativeImage source, Tint tint) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getPixelsABGR();
        if (pixels == null || pixels.length < width * height) {
            return null;
        }
        NativeImage result = new NativeImage(source.format(), width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setPixelABGR(x, y, tintPixel(pixels[y * width + x], tint));
            }
        }
        return result;
    }

    private static int tintPixel(int pixel, Tint tint) {
        int alpha = (pixel >>> 24) & 0xFF;
        if (alpha == 0) {
            return 0;
        }
        int blue = (pixel >>> 16) & 0xFF;
        int green = (pixel >>> 8) & 0xFF;
        int red = pixel & 0xFF;
        // 用原皮肤亮度保留明暗细节，色相统一替换为配置的目标颜色（史莱姆绿）。
        float luminance = (0.299F * red + 0.587F * green + 0.114F * blue) / 255.0F;
        float shade = 0.35F + 0.75F * luminance;
        int outRed = clampChannel(255.0F * tint.red() * shade);
        int outGreen = clampChannel(255.0F * tint.green() * shade);
        int outBlue = clampChannel(255.0F * tint.blue() * shade);
        int outAlpha = Math.round(alpha * tint.alpha());
        return (outAlpha << 24) | (outBlue << 16) | (outGreen << 8) | outRed;
    }

    private static int clampChannel(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static void release(UUID uuid) {
        CachedSkin cached;
        synchronized (CACHE) {
            cached = CACHE.remove(uuid);
        }
        if (cached != null) {
            releaseTexture(cached.tintedPath());
        }
    }

    private static void releaseTexture(Identifier id) {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        if (textures.getTexture(id) != null) {
            textures.release(id);
        }
    }

    private record CachedSkin(Identifier source, String colorKey, Identifier tintedPath, PlayerSkin skin) {}

    private record Tint(float red, float green, float blue, float alpha) {

        private static Tint parse(String capability) {
            String[] parts = capability.split(":");
            if (parts.length != 5) {
                return null;
            }
            try {
                return new Tint(
                    clamp(Float.parseFloat(parts[1])),
                    clamp(Float.parseFloat(parts[2])),
                    clamp(Float.parseFloat(parts[3])),
                    clamp(Float.parseFloat(parts[4])));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static float clamp(float value) {
            return Math.max(0.0F, Math.min(1.0F, value));
        }
    }

    private ModelColorSkinTint() {}
}
