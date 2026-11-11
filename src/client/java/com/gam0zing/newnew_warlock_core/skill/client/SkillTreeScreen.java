package com.gam0zing.newnew_warlock_core.skill.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;
import com.gam0zing.newnew_warlock_core.skill.SkillTreeService;
import com.gam0zing.newnew_warlock_core.skill.net.ResetTreePayload;
import com.gam0zing.newnew_warlock_core.skill.net.SkillProgressSyncPayload;
import com.gam0zing.newnew_warlock_core.skill.net.UnlockNodePayload;
import com.gam0zing.newnew_warlock_core.skill.tree.NodeCommand;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTreeDefinition;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTreeEdge;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTreeNode;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 游戏内技能树界面：只读展示 + 解锁/重置操作，不含编辑功能。
 */
public class SkillTreeScreen extends Screen {

    private static final int NODE_SIZE = 40;
    /** 画布缩放：世界坐标 → 屏幕像素的比例（0.5 表示整棵树缩小一半显示） */
    private static final float CANVAS_SCALE = 0.5F;
    private static final int SCALED_NODE_SIZE = Math.max(1, Math.round(NODE_SIZE * CANVAS_SCALE));
    private static final int TAB_HEIGHT = 28;
    private static final int TAB_WIDTH = 90;
    private static final int COLOR_BACKGROUND = 0xF0101216;
    private static final int COLOR_EDGE = 0xFF3A4250;
    private static final int COLOR_LOCKED = 0xFF2A2F38;
    private static final int COLOR_AVAILABLE = 0xFF3E4A5C;
    private static final int COLOR_UNLOCKED = 0xFF2F6B4F;
    private static final int COLOR_BORDER_LOCKED = 0xFF4A515C;
    private static final int COLOR_BORDER_AVAILABLE = 0xFFD7B45A;
    private static final int COLOR_BORDER_UNLOCKED = 0xFF67D08B;
    private static final int COLOR_BORDER_SELECTED = 0xFFFFFFFF;

    private final List<SkillTreeDefinition> trees = new ArrayList<>();
    private int tab;
    private float panX;
    private float panY;
    @Nullable
    private SkillTreeNode selected;
    @Nullable
    private SkillTreeNode hovered;

    private boolean dragging;
    private double lastMouseX;
    private double lastMouseY;

    private int canvasTop;
    private int canvasBottom;

    public SkillTreeScreen() {
        super(Component.translatable("screen.newnew_warlock_core.skill_tree"));
    }

    @Override
    protected void init() {
        this.trees.clear();
        this.trees.addAll(SkillTreeClientData.sortedTrees());
        this.tab = Math.max(0, Math.min(this.tab, this.trees.size() - 1));
        this.canvasTop = 30;
        this.canvasBottom = this.height - TAB_HEIGHT;
        this.selected = null;
        this.centerOnTree();
    }

    @Nullable
    private SkillTreeDefinition currentTree() {
        return this.trees.isEmpty() ? null : this.trees.get(this.tab);
    }

    private void centerOnTree() {
        SkillTreeDefinition tree = this.currentTree();
        if (tree == null || tree.nodes().isEmpty()) {
            this.panX = this.width / 2.0F;
            this.panY = (this.canvasTop + this.canvasBottom) / 2.0F;
            return;
        }
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (SkillTreeNode node : tree.nodes()) {
            minX = Math.min(minX, node.x());
            maxX = Math.max(maxX, node.x());
            minY = Math.min(minY, node.y());
            maxY = Math.max(maxY, node.y());
        }
        this.panX = this.width / 2.0F - (minX + maxX) / 2.0F * CANVAS_SCALE;
        this.panY = (this.canvasTop + this.canvasBottom) / 2.0F - (minY + maxY) / 2.0F * CANVAS_SCALE;
    }

    private int nodeScreenX(SkillTreeNode node) {
        return Math.round(this.panX + node.x() * CANVAS_SCALE);
    }

    private int nodeScreenY(SkillTreeNode node) {
        return Math.round(this.panY + node.y() * CANVAS_SCALE);
    }

    private boolean isUnlocked(SkillTreeNode node) {
        SkillTreeDefinition tree = this.currentTree();
        return tree != null && SkillTreeClientData.isNodeUnlocked(tree.id(), node.id());
    }

    private boolean prerequisitesSatisfied(SkillTreeNode node) {
        SkillTreeDefinition tree = this.currentTree();
        if (tree == null) {
            return false;
        }
        List<SkillTreeNode> prerequisites = tree.prerequisitesOf(node.id());
        return prerequisites.isEmpty() || prerequisites.stream().anyMatch(this::isUnlocked);
    }

    private double remainingPoints() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0.0D;
        }
        AttributeInstance points = player.getAttribute(ModAttributes.NEWNEW_POINTS);
        return points == null ? 0.0D : points.getValue() - SkillTreeClientData.totalSpent();
    }

    /** {@return 剩余点数是否够解锁该节点；消耗不大于 0 时不检查剩余点数} */
    private boolean canAfford(SkillTreeNode node) {
        double cost = node.cost();
        return cost <= 0.0D || this.remainingPoints() >= cost;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, COLOR_BACKGROUND);

        SkillTreeDefinition tree = this.currentTree();
        if (tree == null) {
            graphics.centeredText(this.font,
                Component.translatable("screen.newnew_warlock_core.skill_tree.empty"),
                this.width / 2, this.height / 2, 0xFFAAAAAA);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        graphics.text(this.font, tree.displayName(), 10, 10, 0xFFFFFFFF);
        graphics.text(this.font,
            Component.translatable("screen.newnew_warlock_core.skill_tree.points", format(remainingPoints())),
            10, 22, 0xFFB8C4D4);

        this.hovered = this.nodeAt(mouseX, mouseY, tree);
        this.drawEdges(graphics, tree);
        this.drawNodes(graphics, tree);
        this.drawTabs(graphics, mouseX, mouseY);
        this.drawResetButton(graphics, mouseX, mouseY, tree);
        this.drawUnlockButton(graphics, mouseX, mouseY);

        if (this.hovered != null) {
            this.drawNodeTooltip(graphics, this.hovered, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawEdges(GuiGraphicsExtractor graphics, SkillTreeDefinition tree) {
        for (SkillTreeEdge edge : tree.edges()) {
            SkillTreeNode from = tree.node(edge.from());
            SkillTreeNode to = tree.node(edge.to());
            if (from == null || to == null) {
                continue;
            }
            int x1 = this.nodeScreenX(from);
            int y1 = this.nodeScreenY(from);
            int x2 = this.nodeScreenX(to);
            int y2 = this.nodeScreenY(to);
            drawArrow(graphics, x1, y1, x2, y2, COLOR_EDGE);
        }
    }

    private void drawNodes(GuiGraphicsExtractor graphics, SkillTreeDefinition tree) {
        for (SkillTreeNode node : tree.nodes()) {
            int centerX = this.nodeScreenX(node);
            int centerY = this.nodeScreenY(node);
            int left = centerX - SCALED_NODE_SIZE / 2;
            int top = centerY - SCALED_NODE_SIZE / 2;
            boolean unlocked = this.isUnlocked(node);
            boolean available = !unlocked && this.prerequisitesSatisfied(node);

            int fill = unlocked ? COLOR_UNLOCKED : available ? COLOR_AVAILABLE : COLOR_LOCKED;
            int border = node == this.selected ? COLOR_BORDER_SELECTED
                : unlocked ? COLOR_BORDER_UNLOCKED : available ? COLOR_BORDER_AVAILABLE : COLOR_BORDER_LOCKED;

            graphics.fill(left - 1, top - 1, left + SCALED_NODE_SIZE + 1, top + SCALED_NODE_SIZE + 1, border);
            graphics.fill(left, top, left + SCALED_NODE_SIZE, top + SCALED_NODE_SIZE, fill);

            if (node.icon() != null) {
                Item item = BuiltInRegistries.ITEM.get(node.icon()).map(Holder::value).orElse(null);
                if (item != null) {
                    ItemStack stack = new ItemStack(item);
                    graphics.item(stack, left + SCALED_NODE_SIZE / 2 - 8, top + SCALED_NODE_SIZE / 2 - 8);
                }
            }
        }
    }

    private void drawTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int y = this.height - TAB_HEIGHT;
        graphics.fill(0, y, this.width, this.height, 0xFF161A20);
        for (int index = 0; index < this.trees.size(); index++) {
            int x = 4 + index * (TAB_WIDTH + 4);
            if (x + TAB_WIDTH > this.width) {
                break;
            }
            boolean active = index == this.tab;
            boolean hover = inRect(mouseX, mouseY, x, y + 4, TAB_WIDTH, TAB_HEIGHT - 8);
            int fill = active ? 0xFF2F3A48 : hover ? 0xFF262E39 : 0xFF1B212A;
            graphics.fill(x, y + 4, x + TAB_WIDTH, y + TAB_HEIGHT - 4, fill);
            graphics.centeredText(this.font, this.trees.get(index).displayName(),
                x + TAB_WIDTH / 2, y + TAB_HEIGHT / 2 - 4, active ? 0xFFFFFFFF : 0xFF9AA6B4);
        }
    }

    private void drawResetButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, SkillTreeDefinition tree) {
        int x = this.width - 90;
        int y = 8;
        boolean hover = inRect(mouseX, mouseY, x, y, 82, 20);
        graphics.fill(x, y, x + 82, y + 20, hover ? 0xFF4A2E2E : 0xFF332222);
        graphics.centeredText(this.font, Component.translatable("screen.newnew_warlock_core.skill_tree.reset"),
            x + 41, y + 6, 0xFFFFB4B4);
        if (hover) {
            SkillProgressSyncPayload.TreeView view = SkillTreeClientData.view(tree.id());
            double spent = view == null ? 0.0D : view.spent();
            int xp = SkillTreeService.resetXpCost(tree, spent);
            graphics.setTooltipForNextFrame(this.font,
                Component.translatable("screen.newnew_warlock_core.skill_tree.reset.tooltip", format(spent), xp),
                mouseX, mouseY);
        }
    }

    private void drawUnlockButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.selected == null || this.isUnlocked(this.selected) || !this.prerequisitesSatisfied(this.selected)) {
            return;
        }
        double cost = this.selected.cost();
        boolean affordable = this.canAfford(this.selected);
        int x = this.width / 2 - 70;
        int y = this.canvasBottom - 32;
        boolean hover = inRect(mouseX, mouseY, x, y, 140, 22);
        int fill = !affordable ? 0xFF171717 : hover ? 0xFF3C5A47 : 0xFF2F4A3A;
        int textColor = !affordable ? 0xFFFF5555 : 0xFFD8FFE6;
        graphics.fill(x, y, x + 140, y + 22, fill);
        graphics.centeredText(this.font,
            Component.translatable("screen.newnew_warlock_core.skill_tree.unlock", format(cost)),
            x + 70, y + 7, textColor);
    }

    private void drawNodeTooltip(GuiGraphicsExtractor graphics, SkillTreeNode node, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(node.name()).withStyle(ChatFormatting.WHITE));
        if (!node.description().isBlank()) {
            lines.add(Component.literal(node.description()).withStyle(ChatFormatting.GRAY));
        }
        // 默认折叠属性明细，按住 Shift 才展开，避免长 tooltip 遮挡画布
        if (!node.commands().isEmpty()) {
            boolean shiftDown = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
            if (shiftDown) {
                for (NodeCommand command : node.commands()) {
                    lines.add(Component.literal("· " + command.renderedSummary()).withStyle(ChatFormatting.AQUA));
                }
            } else {
                lines.add(Component.translatable("screen.newnew_warlock_core.skill_tree.expand_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        lines.add(Component.translatable("screen.newnew_warlock_core.skill_tree.cost", format(node.cost()))
            .withStyle(this.isUnlocked(node) ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        SkillTreeDefinition tree = this.currentTree();
        if (tree == null) {
            return super.mouseClicked(event, doubleClick);
        }

        if (inRect(mouseX, mouseY, this.width - 90, 8, 82, 20)) {
            this.confirmReset(tree);
            return true;
        }

        if (this.selected != null && !this.isUnlocked(this.selected)
            && this.prerequisitesSatisfied(this.selected)) {
            int x = this.width / 2 - 70;
            int y = this.canvasBottom - 32;
            if (inRect(mouseX, mouseY, x, y, 140, 22)) {
                if (this.canAfford(this.selected)) {
                    ClientPacketDistributor.sendToServer(new UnlockNodePayload(tree.id(), this.selected.id()));
                }
                return true;
            }
        }

        int tabIndex = this.tabAt(mouseX, mouseY);
        if (tabIndex >= 0) {
            this.tab = tabIndex;
            this.selected = null;
            this.centerOnTree();
            return true;
        }

        SkillTreeNode node = this.nodeAt(mouseX, mouseY, tree);
        if (node != null) {
            this.selected = node;
            return true;
        }

        this.dragging = true;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (this.dragging && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.panX += (float) (mouseX - this.lastMouseX);
            this.panY += (float) (mouseY - this.lastMouseY);
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.dragging = false;
        return super.mouseReleased(event);
    }

    private void confirmReset(SkillTreeDefinition tree) {
        SkillProgressSyncPayload.TreeView view = SkillTreeClientData.view(tree.id());
        double spent = view == null ? 0.0D : view.spent();
        int xp = SkillTreeService.resetXpCost(tree, spent);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreenAndShow(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                ClientPacketDistributor.sendToServer(new ResetTreePayload(tree.id()));
            }
            minecraft.setScreenAndShow(this);
        },
            Component.translatable("screen.newnew_warlock_core.skill_tree.reset"),
            Component.translatable("screen.newnew_warlock_core.skill_tree.reset.confirm", format(spent), xp)));
    }

    @Nullable
    private SkillTreeNode nodeAt(double mouseX, double mouseY, SkillTreeDefinition tree) {
        for (SkillTreeNode node : tree.nodes()) {
            int left = this.nodeScreenX(node) - SCALED_NODE_SIZE / 2;
            int top = this.nodeScreenY(node) - SCALED_NODE_SIZE / 2;
            if (inRect(mouseX, mouseY, left, top, SCALED_NODE_SIZE, SCALED_NODE_SIZE)) {
                return node;
            }
        }
        return null;
    }

    private int tabAt(double mouseX, double mouseY) {
        int y = this.height - TAB_HEIGHT;
        if (mouseY < y + 4 || mouseY > this.height - 4) {
            return -1;
        }
        for (int index = 0; index < this.trees.size(); index++) {
            int x = 4 + index * (TAB_WIDTH + 4);
            if (mouseX >= x && mouseX <= x + TAB_WIDTH) {
                return index;
            }
        }
        return -1;
    }

    private static boolean inRect(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static void drawArrow(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4D) {
            return;
        }
        double nx = dx / length;
        double ny = dy / length;
        int startX = (int) Math.round(x1 + nx * (SCALED_NODE_SIZE / 2.0D));
        int startY = (int) Math.round(y1 + ny * (SCALED_NODE_SIZE / 2.0D));
        int endX = (int) Math.round(x2 - nx * (SCALED_NODE_SIZE / 2.0D + 4.0D));
        int endY = (int) Math.round(y2 - ny * (SCALED_NODE_SIZE / 2.0D + 4.0D));
        drawLine(graphics, startX, startY, endX, endY, color);

        // 箭头
        double arrowLength = 7.0D;
        double arrowAngle = Math.toRadians(25.0D);
        int leftX = (int) Math.round(endX - arrowLength * (nx * Math.cos(arrowAngle) - ny * Math.sin(arrowAngle)));
        int leftY = (int) Math.round(endY - arrowLength * (ny * Math.cos(arrowAngle) + nx * Math.sin(arrowAngle)));
        int rightX = (int) Math.round(endX - arrowLength * (nx * Math.cos(arrowAngle) + ny * Math.sin(arrowAngle)));
        int rightY = (int) Math.round(endY - arrowLength * (ny * Math.cos(arrowAngle) - nx * Math.sin(arrowAngle)));
        drawLine(graphics, endX, endY, leftX, leftY, color);
        drawLine(graphics, endX, endY, rightX, rightY, color);
    }

    private static void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps <= 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int step = 0; step <= steps; step++) {
            int x = x1 + (x2 - x1) * step / steps;
            int y = y1 + (y2 - y1) * step / steps;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static String format(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
