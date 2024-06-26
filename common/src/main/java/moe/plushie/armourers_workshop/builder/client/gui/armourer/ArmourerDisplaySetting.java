package moe.plushie.armourers_workshop.builder.client.gui.armourer;

import com.apple.library.coregraphics.CGRect;
import com.apple.library.uikit.*;
import com.mojang.authlib.GameProfile;
import moe.plushie.armourers_workshop.builder.blockentity.ArmourerBlockEntity;
import moe.plushie.armourers_workshop.builder.menu.ArmourerMenu;
import moe.plushie.armourers_workshop.builder.network.UpdateArmourerPacket;
import moe.plushie.armourers_workshop.core.texture.PlayerTextureDescriptor;
import moe.plushie.armourers_workshop.core.texture.PlayerTextureLoader;
import moe.plushie.armourers_workshop.init.ModTextures;
import moe.plushie.armourers_workshop.init.platform.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.HashMap;

@Environment(value = EnvType.CLIENT)
public class ArmourerDisplaySetting extends ArmourerBaseSetting implements UITextFieldDelegate {

    protected final ArmourerBlockEntity tileEntity;
    private final HashMap<PlayerTextureDescriptor.Source, String> defaultValues = new HashMap<>();

    private final UIComboBox comboList = new UIComboBox(new CGRect(10, 30, 80, 14));

    private final UITextField textBox = new UITextField(new CGRect(10, 65, 120, 16));
    private final UILabel inputType = new UILabel(new CGRect(10, 55, 160, 10));

    private final UICheckBox checkShowGuides = new UICheckBox(new CGRect(10, 115, 160, 9));
    private final UICheckBox checkShowModelGuides = new UICheckBox(new CGRect(10, 130, 160, 9));
    private final UICheckBox checkShowHelper = new UICheckBox(new CGRect(10, 145, 160, 9));

    private PlayerTextureDescriptor lastDescriptor = PlayerTextureDescriptor.EMPTY;
    private PlayerTextureDescriptor.Source lastSource = PlayerTextureDescriptor.Source.NONE;

    public ArmourerDisplaySetting(ArmourerMenu container) {
        super("inventory.armourers_workshop.armourer.displaySettings");
        this.tileEntity = container.getTileEntity();
        this.reloadData();
    }

    @Override
    public void init() {
        super.init();

        checkShowGuides.setTitle(getDisplayText("showGuide"));
        checkShowGuides.addTarget(this, UIControl.Event.VALUE_CHANGED, ArmourerDisplaySetting::updateFlagValue);
        checkShowModelGuides.setTitle(getDisplayText("showModelGuide"));
        checkShowModelGuides.addTarget(this, UIControl.Event.VALUE_CHANGED, ArmourerDisplaySetting::updateFlagValue);
        checkShowHelper.setTitle(getDisplayText("showHelper"));
        checkShowHelper.addTarget(this, UIControl.Event.VALUE_CHANGED, ArmourerDisplaySetting::updateFlagValue);
        addSubview(checkShowGuides);
        addSubview(checkShowModelGuides);
        addSubview(checkShowHelper);

        UILabel label = new UILabel(new CGRect(10, 20, 160, 10));
        label.setText(getDisplayText("label.skinType"));
        addSubview(label);

        inputType.setText(getDisplayText("label.username"));
        addSubview(inputType);

        String defaultValue = defaultValues.get(lastSource);
        textBox.setMaxLength(1024);
        textBox.setDelegate(this);
        if (Strings.isNotBlank(defaultValue)) {
            textBox.setValue(defaultValue);
        }
        addSubview(textBox);

        UIButton loadBtn = new UIButton(new CGRect(10, 90, 100, 20));
        loadBtn.setTitle(getDisplayText("set"), UIControl.State.ALL);
        loadBtn.setTitleColor(UIColor.WHITE, UIControl.State.ALL);
        loadBtn.setBackgroundImage(ModTextures.defaultButtonImage(), UIControl.State.ALL);
        loadBtn.addTarget(this, UIControl.Event.MOUSE_LEFT_DOWN, ArmourerDisplaySetting::submit);
        addSubview(loadBtn);

        setupComboList(lastSource);

        reloadStatus();
    }

    @Override
    public boolean textFieldShouldReturn(UITextField textField) {
        submit(textField);
        return true;
    }

    @Override
    public void reloadData() {
        prepareDefaultValue();
        reloadStatus();
    }

    private void reloadStatus() {
        if (checkShowGuides == null) {
            return;
        }
        checkShowGuides.setSelected(tileEntity.isShowGuides());
        checkShowModelGuides.setSelected(tileEntity.isShowModelGuides());
        checkShowHelper.setSelected(tileEntity.isShowHelper());
        checkShowHelper.setHidden(!tileEntity.usesHelper());
        // update input type
        if (lastSource == PlayerTextureDescriptor.Source.URL) {
            inputType.setText(getDisplayText("label.url"));
        } else {
            inputType.setText(getDisplayText("label.username"));
        }
    }

    private void prepareDefaultValue() {
        defaultValues.clear();
        if (tileEntity != null) {
            lastDescriptor = tileEntity.getTextureDescriptor();
        }
        lastSource = lastDescriptor.getSource();
        if (lastSource == PlayerTextureDescriptor.Source.USER) {
            defaultValues.put(lastSource, lastDescriptor.getName());
        }
        if (lastSource == PlayerTextureDescriptor.Source.URL) {
            defaultValues.put(lastSource, lastDescriptor.getURL());
        }
    }

    private void submit(Object button) {
        textBox.resignFirstResponder();
        int index = comboList.selectedIndex();
        PlayerTextureDescriptor.Source source = PlayerTextureDescriptor.Source.values()[index + 1];
        applyText(source, textBox.value());
    }

    private void changeSource(PlayerTextureDescriptor.Source newSource) {
        if (this.lastSource == newSource) {
            return;
        }
        defaultValues.put(lastSource, textBox.value());
        textBox.setValue(defaultValues.getOrDefault(newSource, ""));
        textBox.resignFirstResponder();
        //textBox.moveCursorToStart();
        comboList.setSelectedIndex(newSource.ordinal() - 1);
        lastSource = newSource;
        reloadStatus();
    }

    private void applyText(PlayerTextureDescriptor.Source source, String value) {
        PlayerTextureDescriptor descriptor = PlayerTextureDescriptor.EMPTY;
        if (Strings.isNotEmpty(value)) {
            if (source == PlayerTextureDescriptor.Source.URL) {
                descriptor = new PlayerTextureDescriptor(value);
            }
            if (source == PlayerTextureDescriptor.Source.USER) {
                descriptor = new PlayerTextureDescriptor(new GameProfile(null, value));
            }
        }
        PlayerTextureLoader.getInstance().loadTextureDescriptor(descriptor, resolvedDescriptor -> {
            PlayerTextureDescriptor newValue = resolvedDescriptor.orElse(PlayerTextureDescriptor.EMPTY);
            if (lastDescriptor.equals(newValue)) {
                return; // no changes
            }
            lastSource = PlayerTextureDescriptor.Source.NONE;
            lastDescriptor = newValue;
            tileEntity.setTextureDescriptor(newValue);
            UpdateArmourerPacket.Field field = UpdateArmourerPacket.Field.TEXTURE_DESCRIPTOR;
            UpdateArmourerPacket packet = new UpdateArmourerPacket(tileEntity, field, newValue);
            NetworkManager.sendToServer(packet);
            // update to use
            defaultValues.put(newValue.getSource(), newValue.getValue());
            changeSource(newValue.getSource());
        });
    }

    private void updateFlagValue(UIControl sender) {
        int oldFlags = tileEntity.getFlags();
        tileEntity.setShowGuides(checkShowGuides.isSelected());
        tileEntity.setShowModelGuides(checkShowModelGuides.isSelected());
        tileEntity.setShowHelper(checkShowHelper.isSelected());
        int flags = tileEntity.getFlags();
        if (flags == oldFlags) {
            return;
        }
        tileEntity.setFlags(flags);
        UpdateArmourerPacket.Field field = UpdateArmourerPacket.Field.FLAGS;
        UpdateArmourerPacket packet = new UpdateArmourerPacket(tileEntity, field, flags);
        NetworkManager.sendToServer(packet);
    }

    private void setupComboList(PlayerTextureDescriptor.Source source) {
        int selectedIndex = 0;
        if (source != PlayerTextureDescriptor.Source.NONE) {
            selectedIndex = source.ordinal() - 1;
        }
        ArrayList<UIComboItem> items = new ArrayList<>();
        items.add(new UIComboItem(getDisplayText("dropdown.user")));
        items.add(new UIComboItem(getDisplayText("dropdown.url")));
        comboList.setSelectedIndex(selectedIndex);
        comboList.reloadData(items);
        comboList.addTarget(this, UIControl.Event.VALUE_CHANGED, (self, ctr) -> {
            int index = ((UIComboBox) ctr).selectedIndex();
            changeSource(PlayerTextureDescriptor.Source.values()[index + 1]);
        });
        addSubview(comboList);
    }
}
