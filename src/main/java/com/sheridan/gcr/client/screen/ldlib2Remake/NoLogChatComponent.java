package com.sheridan.gcr.client.screen.ldlib2Remake;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@OnlyIn(Dist.CLIENT)
public class NoLogChatComponent {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component DELETED_CHAT_MESSAGE = Component.translatable("chat.deleted_marker").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    private final Minecraft minecraft;
    /**
     * A list of messages previously sent through the chat GUI
     */
    private final ArrayListDeque<String> recentChat = new ArrayListDeque<>(100);
    /**
     * Chat lines to be displayed in the chat box
     */
    private final List<GuiMessage> allMessages = Lists.newArrayList();
    /**
     * List of the ChatLines currently drawn
     */
    private final List<GuiMessage.Line> trimmedMessages = Lists.newArrayList();
    private int chatScrollbarPos;
    private boolean newMessageSinceScroll;
    private final List<NoLogChatComponent.DelayedMessageDeletion> messageDeletionQueue = new ArrayList<>();

    public NoLogChatComponent(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.recentChat.addAll(minecraft.commandHistory().history());
    }

    public void tick() {
        if (!this.messageDeletionQueue.isEmpty()) {
            this.processMessageDeletionQueue();
        }
    }

    public void render(GuiGraphics guiGraphics, int tickCount, int mouseX, int mouseY, boolean focused) {
        if (this.isChatVisible()) {
            int i = this.getLinesPerPage();
            int j = this.trimmedMessages.size();
            if (j > 0) {
                this.minecraft.getProfiler().push("chat");
                float f = (float)this.getScale();
                int k = Mth.ceil((float)this.getWidth() / f);
                int l = guiGraphics.guiHeight();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(f, f, 1.0F);
                guiGraphics.pose().translate(4.0F, 0.0F, 0.0F);
                int i1 = Mth.floor((float)(l - 40) / f);
                int j1 = this.getMessageEndIndexAt(this.screenToChatX((double)mouseX), this.screenToChatY((double)mouseY));
                double d0 = this.minecraft.options.chatOpacity().get() * 0.9F + 0.1F;
                double d1 = this.minecraft.options.textBackgroundOpacity().get();
                double d2 = this.minecraft.options.chatLineSpacing().get();
                int k1 = this.getLineHeight();
                int l1 = (int)Math.round(-8.0 * (d2 + 1.0) + 4.0 * d2);
                int i2 = 0;

                for (int j2 = 0; j2 + this.chatScrollbarPos < this.trimmedMessages.size() && j2 < i; j2++) {
                    int k2 = j2 + this.chatScrollbarPos;
                    GuiMessage.Line guimessage$line = this.trimmedMessages.get(k2);
                    if (guimessage$line != null) {
                        int l2 = tickCount - guimessage$line.addedTime();
                        if (l2 < 200 || focused) {
                            double d3 = focused ? 1.0 : getTimeFactor(l2);
                            int j3 = (int)(255.0 * d3 * d0);
                            int k3 = (int)(255.0 * d3 * d1);
                            i2++;
                            if (j3 > 3) {
                                int l3 = 0;
                                int i4 = i1 - j2 * k1;
                                int j4 = i4 + l1;
                                guiGraphics.fill(-4, i4 - k1, k + 4 + 4, i4, k3 << 24);
                                GuiMessageTag guimessagetag = guimessage$line.tag();
                                if (guimessagetag != null) {
                                    int k4 = guimessagetag.indicatorColor() | j3 << 24;
                                    guiGraphics.fill(-4, i4 - k1, -2, i4, k4);
                                    if (k2 == j1 && guimessagetag.icon() != null) {
                                        int l4 = this.getTagIconLeft(guimessage$line);
                                        int i5 = j4 + 9;
                                        this.drawTagIcon(guiGraphics, l4, i5, guimessagetag.icon());
                                    }
                                }

                                guiGraphics.pose().pushPose();
                                guiGraphics.pose().translate(0.0F, 0.0F, 50.0F);
                                guiGraphics.drawString(this.minecraft.font, guimessage$line.content(), 0, j4, 16777215 + (j3 << 24));
                                guiGraphics.pose().popPose();
                            }
                        }
                    }
                }

                long j5 = this.minecraft.getChatListener().queueSize();
                if (j5 > 0L) {
                    int k5 = (int)(128.0 * d0);
                    int i6 = (int)(255.0 * d1);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0.0F, (float)i1, 0.0F);
                    guiGraphics.fill(-2, 0, k + 4, 9, i6 << 24);
                    guiGraphics.pose().translate(0.0F, 0.0F, 50.0F);
                    guiGraphics.drawString(this.minecraft.font, Component.translatable("chat.queue", j5), 0, 1, 16777215 + (k5 << 24));
                    guiGraphics.pose().popPose();
                }

                if (focused) {
                    int l5 = this.getLineHeight();
                    int j6 = j * l5;
                    int k6 = i2 * l5;
                    int i3 = this.chatScrollbarPos * k6 / j - i1;
                    int l6 = k6 * k6 / j6;
                    if (j6 != k6) {
                        int i7 = i3 > 0 ? 170 : 96;
                        int j7 = this.newMessageSinceScroll ? 13382451 : 3355562;
                        int k7 = k + 4;
                        guiGraphics.fill(k7, -i3, k7 + 2, -i3 - l6, 100, j7 + (i7 << 24));
                        guiGraphics.fill(k7 + 2, -i3, k7 + 1, -i3 - l6, 100, 13421772 + (i7 << 24));
                    }
                }

                guiGraphics.pose().popPose();
                this.minecraft.getProfiler().pop();
            }
        }
    }

    private void drawTagIcon(GuiGraphics guiGraphics, int left, int bottom, GuiMessageTag.Icon tagIcon) {
        int i = bottom - tagIcon.height - 1;
        tagIcon.draw(guiGraphics, left, i);
    }

    private int getTagIconLeft(GuiMessage.Line line) {
        return this.minecraft.font.width(line.content()) + 4;
    }

    private boolean isChatVisible() {
        return this.minecraft.options.chatVisibility().get() != ChatVisiblity.HIDDEN;
    }

    private static double getTimeFactor(int counter) {
        double d0 = (double)counter / 200.0;
        d0 = 1.0 - d0;
        d0 *= 10.0;
        d0 = Mth.clamp(d0, 0.0, 1.0);
        return d0 * d0;
    }

    /**
     * Clears the chat.
     *
     * @param clearSentMsgHistory Whether to clear the user's sent message history
     */
    public void clearMessages(boolean clearSentMsgHistory) {
        this.minecraft.getChatListener().clearQueue();
        this.messageDeletionQueue.clear();
        this.trimmedMessages.clear();
        this.allMessages.clear();
        if (clearSentMsgHistory) {
            this.recentChat.clear();
            this.recentChat.addAll(this.minecraft.commandHistory().history());
        }
    }

    public void addMessage(Component chatComponent) {
        this.addMessage(chatComponent, null, this.minecraft.isSingleplayer() ? GuiMessageTag.systemSinglePlayer() : GuiMessageTag.system());
    }

    public void addMessage(Component chatComponent, @Nullable MessageSignature headerSignature, @Nullable GuiMessageTag tag) {
        GuiMessage guimessage = new GuiMessage(this.minecraft.gui.getGuiTicks(), chatComponent, headerSignature, tag);
        this.addMessageToDisplayQueue(guimessage);
        this.addMessageToQueue(guimessage);
    }

    private void addMessageToDisplayQueue(GuiMessage message) {
        int i = Mth.floor((double)this.getWidth() / this.getScale());
        GuiMessageTag.Icon guimessagetag$icon = message.icon();
        if (guimessagetag$icon != null) {
            i -= guimessagetag$icon.width + 4 + 2;
        }

        List<FormattedCharSequence> list = ComponentRenderUtils.wrapComponents(message.content(), i, this.minecraft.font);
        boolean flag = this.isChatFocused();

        for (int j = 0; j < list.size(); j++) {
            FormattedCharSequence formattedcharsequence = list.get(j);
            if (flag && this.chatScrollbarPos > 0) {
                this.newMessageSinceScroll = true;
                this.scrollChat(1);
            }

            boolean flag1 = j == list.size() - 1;
            this.trimmedMessages.addFirst(new GuiMessage.Line(message.addedTime(), formattedcharsequence, message.tag(), flag1));
        }

        while (this.trimmedMessages.size() > 100) {
            this.trimmedMessages.removeLast();
        }
    }

    private void addMessageToQueue(GuiMessage message) {
        this.allMessages.addFirst(message);

        while (this.allMessages.size() > 100) {
            this.allMessages.removeLast();
        }
    }

    private void processMessageDeletionQueue() {
        int i = this.minecraft.gui.getGuiTicks();
        this.messageDeletionQueue.removeIf(p_250713_ -> i >= p_250713_.deletableAfter() && this.deleteMessageOrDelay(p_250713_.signature()) == null);
    }

    public void deleteMessage(MessageSignature messageSignature) {
        NoLogChatComponent.DelayedMessageDeletion chatcomponent$delayedmessagedeletion = this.deleteMessageOrDelay(messageSignature);
        if (chatcomponent$delayedmessagedeletion != null) {
            this.messageDeletionQueue.add(chatcomponent$delayedmessagedeletion);
        }
    }

    @Nullable
    private NoLogChatComponent.DelayedMessageDeletion deleteMessageOrDelay(MessageSignature messageSignature) {
        int i = this.minecraft.gui.getGuiTicks();
        ListIterator<GuiMessage> listiterator = this.allMessages.listIterator();

        while (listiterator.hasNext()) {
            GuiMessage guimessage = listiterator.next();
            if (messageSignature.equals(guimessage.signature())) {
                int j = guimessage.addedTime() + 60;
                if (i >= j) {
                    listiterator.set(this.createDeletedMarker(guimessage));
                    this.refreshTrimmedMessages();
                    return null;
                }

                return new NoLogChatComponent.DelayedMessageDeletion(messageSignature, j);
            }
        }

        return null;
    }

    private GuiMessage createDeletedMarker(GuiMessage message) {
        return new GuiMessage(message.addedTime(), DELETED_CHAT_MESSAGE, null, GuiMessageTag.system());
    }

    public void rescaleChat() {
        this.resetChatScroll();
        this.refreshTrimmedMessages();
    }

    private void refreshTrimmedMessages() {
        this.trimmedMessages.clear();

        for (GuiMessage guimessage : Lists.reverse(this.allMessages)) {
            this.addMessageToDisplayQueue(guimessage);
        }
    }

    public ArrayListDeque<String> getRecentChat() {
        return this.recentChat;
    }

    /**
     * Adds this string to the list of sent messages, for recall using the up/down arrow keys
     */
    public void addRecentChat(String message) {
        if (!message.equals(this.recentChat.peekLast())) {
            if (this.recentChat.size() >= 100) {
                this.recentChat.removeFirst();
            }

            this.recentChat.addLast(message);
        }

        if (message.startsWith("/")) {
            this.minecraft.commandHistory().addCommand(message);
        }
    }

    public void resetChatScroll() {
        this.chatScrollbarPos = 0;
        this.newMessageSinceScroll = false;
    }

    public void scrollChat(int posInc) {
        this.chatScrollbarPos += posInc;
        int i = this.trimmedMessages.size();
        if (this.chatScrollbarPos > i - this.getLinesPerPage()) {
            this.chatScrollbarPos = i - this.getLinesPerPage();
        }

        if (this.chatScrollbarPos <= 0) {
            this.chatScrollbarPos = 0;
            this.newMessageSinceScroll = false;
        }
    }

    public boolean handleChatQueueClicked(double mouseX, double mouseY) {
        if (this.isChatFocused() && !this.minecraft.options.hideGui && this.isChatVisible()) {
            ChatListener chatlistener = this.minecraft.getChatListener();
            if (chatlistener.queueSize() == 0L) {
                return false;
            } else {
                double d0 = mouseX - 2.0;
                double d1 = (double)this.minecraft.getWindow().getGuiScaledHeight() - mouseY - 40.0;
                if (d0 <= (double)Mth.floor((double)this.getWidth() / this.getScale()) && d1 < 0.0 && d1 > (double)Mth.floor(-9.0 * this.getScale())) {
                    chatlistener.acceptNextDelayedMessage();
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    @Nullable
    public Style getClickedComponentStyleAt(double mouseX, double mouseY) {
        double d0 = this.screenToChatX(mouseX);
        double d1 = this.screenToChatY(mouseY);
        int i = this.getMessageLineIndexAt(d0, d1);
        if (i >= 0 && i < this.trimmedMessages.size()) {
            GuiMessage.Line guimessage$line = this.trimmedMessages.get(i);
            return this.minecraft.font.getSplitter().componentStyleAtWidth(guimessage$line.content(), Mth.floor(d0));
        } else {
            return null;
        }
    }

    @Nullable
    public GuiMessageTag getMessageTagAt(double mouseX, double mouseY) {
        double d0 = this.screenToChatX(mouseX);
        double d1 = this.screenToChatY(mouseY);
        int i = this.getMessageEndIndexAt(d0, d1);
        if (i >= 0 && i < this.trimmedMessages.size()) {
            GuiMessage.Line guimessage$line = this.trimmedMessages.get(i);
            GuiMessageTag guimessagetag = guimessage$line.tag();
            if (guimessagetag != null && this.hasSelectedMessageTag(d0, guimessage$line, guimessagetag)) {
                return guimessagetag;
            }
        }

        return null;
    }

    private boolean hasSelectedMessageTag(double x, GuiMessage.Line line, GuiMessageTag tag) {
        if (x < 0.0) {
            return true;
        } else {
            GuiMessageTag.Icon guimessagetag$icon = tag.icon();
            if (guimessagetag$icon == null) {
                return false;
            } else {
                int i = this.getTagIconLeft(line);
                int j = i + guimessagetag$icon.width;
                return x >= (double)i && x <= (double)j;
            }
        }
    }

    private double screenToChatX(double x) {
        return x / this.getScale() - 4.0;
    }

    private double screenToChatY(double y) {
        double d0 = (double)this.minecraft.getWindow().getGuiScaledHeight() - y - 40.0;
        return d0 / (this.getScale() * (double)this.getLineHeight());
    }

    private int getMessageEndIndexAt(double mouseX, double mouseY) {
        int i = this.getMessageLineIndexAt(mouseX, mouseY);
        if (i == -1) {
            return -1;
        } else {
            while (i >= 0) {
                if (this.trimmedMessages.get(i).endOfEntry()) {
                    return i;
                }

                i--;
            }

            return i;
        }
    }

    private int getMessageLineIndexAt(double mouseX, double mouseY) {
        if (this.isChatFocused() && this.isChatVisible()) {
            if (!(mouseX < -4.0) && !(mouseX > (double)Mth.floor((double)this.getWidth() / this.getScale()))) {
                int i = Math.min(this.getLinesPerPage(), this.trimmedMessages.size());
                if (mouseY >= 0.0 && mouseY < (double)i) {
                    int j = Mth.floor(mouseY + (double)this.chatScrollbarPos);
                    if (j >= 0 && j < this.trimmedMessages.size()) {
                        return j;
                    }
                }

            }
        }
        return -1;
    }

    public boolean isChatFocused() {
        return this.minecraft.screen instanceof ChatScreen;
    }

    public int getWidth() {
        return getWidth(this.minecraft.options.chatWidth().get());
    }

    public int getHeight() {
        return getHeight(this.isChatFocused() ? this.minecraft.options.chatHeightFocused().get() : this.minecraft.options.chatHeightUnfocused().get());
    }

    public double getScale() {
        return this.minecraft.options.chatScale().get();
    }

    public static int getWidth(double width) {
        return Mth.floor(width * 280.0 + 40.0);
    }

    public static int getHeight(double height) {
        return Mth.floor(height * 160.0 + 20.0);
    }
    public int getLinesPerPage() {
        return this.getHeight() / this.getLineHeight();
    }

    private int getLineHeight() {
        return (int)(9.0 * (this.minecraft.options.chatLineSpacing().get() + 1.0));
    }

    public NoLogChatComponent.State storeState() {
        return new NoLogChatComponent.State(List.copyOf(this.allMessages), List.copyOf(this.recentChat), List.copyOf(this.messageDeletionQueue));
    }

    public void restoreState(NoLogChatComponent.State state) {
        this.recentChat.clear();
        this.recentChat.addAll(state.history);
        this.messageDeletionQueue.clear();
        this.messageDeletionQueue.addAll(state.delayedMessageDeletions);
        this.allMessages.clear();
        this.allMessages.addAll(state.messages);
        this.refreshTrimmedMessages();
    }

    @OnlyIn(Dist.CLIENT)
    static record DelayedMessageDeletion(MessageSignature signature, int deletableAfter) {
    }

    @OnlyIn(Dist.CLIENT)
    public static class State {
        final List<GuiMessage> messages;
        final List<String> history;
        final List<NoLogChatComponent.DelayedMessageDeletion> delayedMessageDeletions;

        public State(List<GuiMessage> messages, List<String> history, List<NoLogChatComponent.DelayedMessageDeletion> delayedMessageDeletions) {
            this.messages = messages;
            this.history = history;
            this.delayedMessageDeletions = delayedMessageDeletions;
        }
    }
}
