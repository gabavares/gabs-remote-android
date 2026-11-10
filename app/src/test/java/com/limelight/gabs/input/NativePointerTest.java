package com.limelight.gabs.input;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.view.PointerIcon;

import org.junit.Test;

public class NativePointerTest {
    @Test
    public void formatosViramIconesDoAndroid() {
        assertEquals(PointerIcon.TYPE_ARROW, NativePointer.iconType(CursorShape.ARROW));
        assertEquals(PointerIcon.TYPE_TEXT, NativePointer.iconType(CursorShape.IBEAM));
        assertEquals(PointerIcon.TYPE_HAND, NativePointer.iconType(CursorShape.HAND));
        assertEquals(PointerIcon.TYPE_WAIT, NativePointer.iconType(CursorShape.WAIT));
        assertEquals(PointerIcon.TYPE_WAIT, NativePointer.iconType(CursorShape.APP_STARTING));
        assertEquals(PointerIcon.TYPE_CROSSHAIR, NativePointer.iconType(CursorShape.CROSS));
        assertEquals(PointerIcon.TYPE_VERTICAL_DOUBLE_ARROW, NativePointer.iconType(CursorShape.SIZE_NS));
        assertEquals(PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW, NativePointer.iconType(CursorShape.SIZE_WE));
        assertEquals(PointerIcon.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW, NativePointer.iconType(CursorShape.SIZE_NWSE));
        assertEquals(PointerIcon.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW, NativePointer.iconType(CursorShape.SIZE_NESW));
        assertEquals(PointerIcon.TYPE_ALL_SCROLL, NativePointer.iconType(CursorShape.SIZE_ALL));
        assertEquals(PointerIcon.TYPE_NO_DROP, NativePointer.iconType(CursorShape.NO));
        assertEquals(PointerIcon.TYPE_HELP, NativePointer.iconType(CursorShape.HELP));
        assertEquals(PointerIcon.TYPE_ARROW, NativePointer.iconType(CursorShape.OTHER));
        assertEquals(PointerIcon.TYPE_NULL, NativePointer.iconType(CursorShape.HIDDEN));
    }

    @Test
    public void doisDedosRolamComoNoTrackpadDoWindows() {
        NativePointer.SwipeScroll s = new NativePointer.SwipeScroll();
        s.start(100, 100);
        // Dedos descem 40 px: o conteúdo desce junto (rola para cima) um dente da roda.
        assertArrayEquals(new int[]{120, 0}, s.move(100, 140));
        // Para a direita 20 px: meio dente para a esquerda.
        assertArrayEquals(new int[]{0, -60}, s.move(120, 140));
    }

    @Test
    public void rolagemPequenaAcumula() {
        NativePointer.SwipeScroll s = new NativePointer.SwipeScroll();
        s.start(0, 0);
        assertArrayEquals(new int[]{1, 0}, s.move(0, 0.4f)); // 1,2 unidades
        assertArrayEquals(new int[]{1, 0}, s.move(0, 0.8f)); // 1,2 + 0,2 de resto
    }
}
