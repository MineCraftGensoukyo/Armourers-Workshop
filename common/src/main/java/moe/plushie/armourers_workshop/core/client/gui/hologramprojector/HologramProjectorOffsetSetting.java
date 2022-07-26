package moe.plushie.armourers_workshop.core.client.gui.hologramprojector;

import com.mojang.blaze3d.vertex.PoseStack;
import moe.plushie.armourers_workshop.core.blockentity.HologramProjectorBlockEntity;
import moe.plushie.armourers_workshop.core.client.gui.widget.AWSliderBox;
import moe.plushie.armourers_workshop.core.client.gui.widget.AWTabPanel;
import moe.plushie.armourers_workshop.core.network.UpdateHologramProjectorPacket;
import moe.plushie.armourers_workshop.init.platform.NetworkManager;
import moe.plushie.armourers_workshop.utils.RenderUtils;
import moe.plushie.armourers_workshop.utils.TranslateUtils;
import moe.plushie.armourers_workshop.utils.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

@Environment(value = EnvType.CLIENT)
public class HologramProjectorOffsetSetting extends AWTabPanel {

    private final HologramProjectorBlockEntity entity;
    private final UpdateHologramProjectorPacket.Field field = UpdateHologramProjectorPacket.Field.OFFSET;
    protected int contentWidth = 200;
    protected int contentHeight = 82;
    private AWSliderBox sliderX;
    private AWSliderBox sliderY;
    private AWSliderBox sliderZ;

    public HologramProjectorOffsetSetting(HologramProjectorBlockEntity entity) {
        super("inventory.armourers_workshop.hologram-projector.offset");
        this.entity = entity;
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);

        int top = 0;
        int left = (width - 178) / 2;

        this.sliderX = addSlider(left, top + 30, 178, 10, "X: ");
        this.sliderY = addSlider(left, top + 45, 178, 10, "Y: ");
        this.sliderZ = addSlider(left, top + 60, 178, 10, "Z: ");

        Vector3f value = field.get(entity);
        this.sliderX.setValue(value.x());
        this.sliderY.setValue(value.y());
        this.sliderZ.setValue(value.z());
    }

    @Override
    public void removed() {
        sliderZ = null;
        sliderX = null;
        sliderY = null;
        super.removed();
    }


    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int x = (width - contentWidth) / 2;
        RenderUtils.bind(RenderUtils.TEX_HOLOGRAM_PROJECTOR);
        RenderUtils.drawContinuousTexturedBox(matrixStack, x, 0, 0, 138, contentWidth, contentHeight, 38, 38, 4, 0);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private AWSliderBox addSlider(int x, int y, int width, int height, String key) {
        Function<Double, Component> titleProvider = currentValue -> {
            String formattedValue = String.format("%s%.0f", key, currentValue);
            return TranslateUtils.literal(formattedValue);
        };
        AWSliderBox slider = new AWSliderBox(x, y, width, height, titleProvider, -64, 64, this::updateValue);
        slider.setEndListener(this::didUpdateValue);
        addButton(slider);
        return slider;
    }

    private void updateValue(Button button) {
        float x = (float) sliderX.getValue();
        float y = (float) sliderY.getValue();
        float z = (float) sliderZ.getValue();
        field.set(entity, new Vector3f(x, y, z));
    }

    private void didUpdateValue(Button button) {
        float x = (float) sliderX.getValue();
        float y = (float) sliderY.getValue();
        float z = (float) sliderZ.getValue();
        UpdateHologramProjectorPacket packet = new UpdateHologramProjectorPacket(entity, field, new Vector3f(x, y, z));
        NetworkManager.sendToServer(packet);
    }
}