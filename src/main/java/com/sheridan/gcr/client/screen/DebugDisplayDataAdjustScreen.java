package com.sheridan.gcr.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.items.DisplayData;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.ModuleHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class DebugDisplayDataAdjustScreen extends Screen {
    private static final Map<GunItem, DisplayData> historyDataCache = new HashMap<>();
    static Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private EditBox editBox;
    public DisplayData displayData;
    private DisplayData historyData;
    private int viewIndex = 0;
    private int operationIndex = 0;
    private float p = 0.1f;
    public int mx, my;
    private static final String[] viewModeNames = {"FirstPersonMain", "ThirdPersonRight", "Ground","Frame", "AttachmentScreen", "Sprinting"};
    private Button pos, rot, scale;

    public DebugDisplayDataAdjustScreen() {
        super(Component.literal(""));

    }
    protected void init() {
        super.init();
        GridLayout gridlayout = new GridLayout();
        gridlayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper rowHelper = gridlayout.createRowHelper(2);
        rowHelper.addChild(Button.builder(Component.literal("close"), (p_280814_) -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
                this.minecraft.mouseHandler.grabMouse();
            }
        }).width(50).build(), 2, gridlayout.newCellSettings().paddingTop(50));
        editBox = new EditBox(Minecraft.getInstance().font, 50, 150, 100, 20, Component.literal("p"));
        editBox.setValue("0.1");
        rowHelper.addChild(editBox);
        initBtn(rowHelper);
        gridlayout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (viewIndex == 4) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.getMainHandItem().getItem() instanceof GunItem gunItem) {
                ItemStack mainHandItem = player.getMainHandItem();

                Client.getGunRenderer().renderGunModifyScreen(
                        mainHandItem,
                        gunItem.getGun(),
                        ModuleHandler.buildRenderTree(mainHandItem),
                        0, 0, 0, 0, 1, null);

            }
        }

        for(Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        Utils.drawDevHelperLayers(guiGraphics, this.font, mouseX, mouseY, this.width, this.height, 0, 0);
    }

    private void setOperationIndex(int index) {
        operationIndex = index;
        switch (operationIndex) {
            case 0: {
                pos.setFGColor(0xffff00);
                rot.setFGColor(0xffffff);
                scale.setFGColor(0xffffff);
                break;
            }
            case 1: {
                rot.setFGColor(0xffff00);
                pos.setFGColor(0xffffff);
                scale.setFGColor(0xffffff);
                break;
            }
            case 2: {
                scale.setFGColor(0xffff00);
                pos.setFGColor(0xffffff);
                rot.setFGColor(0xffffff);
                break;
            }
        }
    }

    private void initBtn(GridLayout.RowHelper rowHelper) {
        pos = Button.builder(Component.literal("pos"), (btn) -> setOperationIndex(0)).width(40).pos(50, 20).build();
        pos.setFGColor(0xffff00);
        rot = Button.builder(Component.literal("rot"), (btn) -> setOperationIndex(1)).width(40).pos(90, 20).build();
        scale = Button.builder(Component.literal("scale"), (btn) -> setOperationIndex(2)).width(40).pos(130, 20).build();
        rowHelper.addChild(pos);
        rowHelper.addChild(rot);
        rowHelper.addChild(scale);
        rowHelper.addChild(Button.builder(Component.literal("FirstPersonMain"), (btn) -> {
            viewIndex ++;
            viewIndex %= viewModeNames.length;
            btn.setMessage(Component.literal(viewModeNames[viewIndex]));
            
        }).width(100).pos(300, 20).build());
        rowHelper.addChild(Button.builder(Component.literal("Bobbing"), (btn) -> Client.handleWeaponBobbing = !Client.handleWeaponBobbing).width(50).pos(300, 50).build());
        rowHelper.addChild(Button.builder(Component.literal("print"), (btn) -> printJavaCodeToConsole()).width(100).pos(50, 180).build());
        for (int i = 0; i < 3; i ++) {
            String name = switch (i) {
                case 1 -> "y";
                case 2 -> "z";
                default -> "x";
            };
            int finalI = i;
            rowHelper.addChild(Button.builder(Component.literal(name + "+"), (btn) ->{
                add(finalI);
            }).width(30).pos(50, 45 + 20 * i).build());
            rowHelper.addChild(Button.builder(Component.literal(name + "-"), (btn) -> {
                dec(finalI);
            }).width(30).pos(90, 45 + 20 * i).build());
            rowHelper.addChild(Button.builder(Component.literal("reset"), (btn) -> {
                reset(finalI);
            }).width(35).pos(130, 45 + 20 * i).build());
        }
        rowHelper.addChild(Button.builder(Component.literal("copy"), (btn) -> {
            if (displayData != null) {
                JsonObject jsonObject = new JsonObject();
                displayData.writeToJson(jsonObject);
                String s = GSON.toJson(jsonObject);
                Minecraft.getInstance().keyboardHandler.setClipboard(s);
                System.out.println(s);
            }
        }).width(100).pos(155, 180).build());

        rowHelper.addChild(Button.builder(Component.literal("✓"), (btn) -> {
            try {
                p = Math.abs(Float.parseFloat(editBox.getValue()));
            } catch (Exception e) {
                editBox.setValue(p + "");
            }
        }).width(20).pos(155, 150).build());

    }

    private void add(int index) {
        if (displayData != null) {
            displayData.inc(viewIndex, operationIndex * 3 + index, getP());
        }
    }

    private float getP() {
        if (operationIndex == 0) {
            return p / 16f;
        } else if (operationIndex == 1) {
            return (float) Math.toRadians(p);
        } else if (operationIndex == 2) {
            return p;
        }
        return 0;
    }

    private void reset(int index) {
        if (displayData != null) {
            int i = operationIndex * 3 + index;
            displayData.set(viewIndex, i, historyData.get(viewIndex, i));
        }
    }

    private void dec(int index) {
        if (displayData != null) {
            displayData.dec(viewIndex, operationIndex * 3 + index, p / 16f);
        }
    }

    private void printJavaCodeToConsole() {
        if (displayData != null) {
            System.out.println(displayData.getJavaInitCode());
        }
    }

    @Override
    public void mouseMoved(double pMouseX, double pMouseY) {
        super.mouseMoved(pMouseX, pMouseY);
        mx = (int) pMouseX;
        my = (int) pMouseY;
    }

    @Override
    public void tick() {
        super.tick();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            this.onClose();
            return;
        }
        ItemStack itemStack = player.getMainHandItem();
        if (itemStack.getItem() instanceof GunItem gunItem) {
            if (displayData == null) {
                if (!historyDataCache.containsKey(gunItem)) {
                    historyDataCache.put(gunItem, gunItem.getGun().getDisplayData().copy());
                }
                historyData = historyDataCache.get(gunItem);
            }
            displayData = gunItem.getGun().getDisplayData();
            Client.getGunRenderer().setHideFPRender(viewIndex == 4);
        } else {
            this.onClose();
        }
    }



    @Override
    public void onClose() {
        super.onClose();
        Client.getGunRenderer().setHideFPRender(false);
    }
}
