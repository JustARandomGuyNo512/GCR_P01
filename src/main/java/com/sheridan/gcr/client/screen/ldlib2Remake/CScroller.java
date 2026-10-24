package com.sheridan.gcr.client.screen.ldlib2Remake;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Scroller;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class CScroller extends Scroller.Horizontal{
    private Function<Float, Boolean> preValueChange = (f) -> true;


    public CScroller setPreValueChange(Function<Float, Boolean> preValueChange) {
        this.preValueChange = preValueChange;
        return this;
    }


    @Override
    protected void onDraggingScrollBar(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof Float initialValue) {
            var minX = scrollContainer.getContentX();
            var maxX = scrollContainer.getContentX() + scrollContainer.getContentWidth();

            var remainingSpace = maxX - minX - scrollBar.getSizeWidth();
            var localMouse = getLocalMouse(event.x, event.y);
            var localStart = getLocalMouse(event.dragStartX, event.dragStartY);
            var deltaX = localMouse.x - localStart.x;
            var distValue = (deltaX / remainingSpace) * (maxValue - minValue);
            var newValue = distValue + initialValue;
            if (!preValueChange.apply(newValue)) {
                return;
            }
            setValue(newValue);
        }
    }

    @Override
    protected void clickScrollContainer(@NotNull UIEvent event) {}

    @Override
    protected void onScrollWheel(@NotNull UIEvent event) {}
}
