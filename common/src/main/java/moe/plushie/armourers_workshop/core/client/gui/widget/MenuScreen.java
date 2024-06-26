package moe.plushie.armourers_workshop.core.client.gui.widget;

import com.apple.library.coregraphics.CGGraphicsContext;
import com.apple.library.coregraphics.CGRect;
import com.apple.library.foundation.NSString;
import com.apple.library.uikit.UIFont;
import com.apple.library.uikit.UIWindow;
import com.apple.library.uikit.UIWindowManager;
import me.sagesse.minecraft.client.gui.ContainerMenuScreen;
import moe.plushie.armourers_workshop.api.common.IMenuScreenProvider;
import moe.plushie.armourers_workshop.api.common.IMenuWindow;
import moe.plushie.armourers_workshop.api.common.IMenuWindowProvider;
import moe.plushie.armourers_workshop.api.math.IPoseStack;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

@Environment(value = EnvType.CLIENT)
public class MenuScreen<M extends AbstractContainerMenu, W extends UIWindow & IMenuWindow<M>> extends ContainerMenuScreen<M> {

    private UIFont font;

    private final W window;
    private final MenuWindow<?> menuWindow;
    private final UIWindowManager manager;

    public MenuScreen(W window, M menu, Inventory inventory, Component component) {
        super(menu, inventory, component);

        this.window = window;
        this.menuWindow = ObjectUtils.safeCast(window, MenuWindow.class);

        this.manager = new UIWindowManager();
        this.manager.addWindow(window);
        this.manager.init();
    }

    public static <M extends AbstractContainerMenu, W extends UIWindow & IMenuWindow<M>> IMenuScreenProvider<M, MenuScreen<M, W>> bind(IMenuWindowProvider<M, W> provider) {
        return (menu, inv, title) -> {
            W window = provider.createMenuWindow(menu, inv, new NSString(title));
            return new MenuScreen<>(window, menu, inv, title);
        };
    }

    @Override
    protected void init() {
        font = new UIFont(super.font);
        manager.layout(width, height);
        CGRect rect = window.bounds();
        imageWidth = rect.width;
        imageHeight = rect.height;
        super.init();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        menuWindow.screenWillTick();
    }

    @Override
    public void removed() {
        super.removed();
        manager.deinit();
    }

    @Override
    public void render(IPoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        CGGraphicsContext context = new CGGraphicsContext(poseStack, mouseX, mouseY, partialTicks, font, this);
        manager.tick();
        manager.render(context, this::_render, this::_renderBackground, this::_renderTooltip);
    }

    @Override
    public void renderBg(IPoseStack poseStack, float partialTicks, int mouseX, int mouseY) {
        // ignored
    }

    @Override
    public void renderLabels(IPoseStack poseStack, int mouseX, int mouseY) {
        // ignored
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return manager.mouseDown(mouseX, mouseY, button, this::_mouseClicked);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return manager.mouseUp(mouseX, mouseY, button, super::mouseReleased);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return manager.mouseWheel(mouseX, mouseY, delta, super::mouseScrolled);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        manager.mouseMoved(mouseX, mouseY, 0, this::_mouseMoved);
    }

    @Override
    public boolean keyPressed(int key, int i, int j) {
        return manager.keyDown(key, i, j, this::_keyPressed);
    }

    @Override
    public boolean keyReleased(int key, int i, int j) {
        return manager.keyUp(key, i, j, super::keyReleased);
    }

    @Override
    public boolean charTyped(char ch, int i) {
        return manager.charTyped(ch, i, 0, this::_charTyped);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return !manager.mouseIsInside(mouseX, mouseY, button);
    }

    @Override
    protected void slotClicked(Slot slot, int slotIndex, int j, ClickType clickType) {
        super.slotClicked(slot, slotIndex, j, clickType);
        // in normal case the clicking will call the `slotClicked`,
        // we need to know that this is a slot click.
        if (slotIndex >= 0) {
            window.menuDidChange();
        }
    }

    @Override
    public boolean changeFocus(boolean bl) {
        return manager.changeKeyView(bl);
    }

    public boolean shouldDrawPluginScreen() {
        if (menuWindow != null) {
            return menuWindow.shouldRenderExtendScreen();
        }
        return false;
    }

    private void _render(int mouseX, int mouseY, float partialTicks, CGGraphicsContext context) {
        super.render(context.poseStack, mouseX, mouseY, partialTicks);
    }

    private void _renderTooltip(int mouseX, int mouseY, float partialTicks, CGGraphicsContext context) {
        IPoseStack poseStack = context.poseStack;
        poseStack.pushPose();
        poseStack.translate(0, 0, 400);
        renderTooltip(context.poseStack.cast(), mouseX, mouseY);
        poseStack.popPose();
    }

    private void _renderBackground(int mouseX, int mouseY, float partialTicks, CGGraphicsContext context) {
        // draw bg
        if (menuWindow != null && menuWindow.shouldRenderBackground()) {
            renderBackground(context.poseStack.cast());
        }
    }

    private boolean _charTyped(int key, int i, int j) {
        super.charTyped((char) key, i);
        return true;
    }

    private boolean _keyPressed(int key, int i, int j) {
        // when input first responder is actived, the shortcut key events not allowed.
        if (manager.isTextEditing() && !_editingPassKey((char) key)) {
            return false;
        }
        return super.keyPressed(key, i, j);
    }

    private boolean _mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean _mouseMoved(double mouseX, double mouseY, int button) {
        super.mouseMoved(mouseX, mouseY);
        return true;
    }


    private boolean _editingPassKey(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE:
            case GLFW.GLFW_KEY_TAB:
                return true;

            default:
                return false;
        }
    }
}
