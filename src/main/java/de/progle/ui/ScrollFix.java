package de.progle.ui;

import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;

/**
 * Leitet das Mausrad von verschachtelten scrollbaren Steuerelementen
 * (Code-Editor, Textfelder, Listen) an den uebergeordneten ScrollPane weiter,
 * damit die ganze Seite immer mit dem Rad gescrollt werden kann.
 */
public final class ScrollFix {

    private ScrollFix() {
    }

    public static void forwardTo(Node inner, ScrollPane outer) {
        inner.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) {
                return;
            }
            scrollOuter(outer, event.getDeltaY(), event.isControlDown() ? 3 : 1);
            event.consume();
        });
    }

    /**
     * ListView innerhalb eines aeusseren ScrollPanes (Sidebar):
     * Das Rad scrollt zuerst die Liste; ist deren Anfang bzw. Ende erreicht,
     * laeuft das Rad an den aeusseren ScrollPane weiter (Ketten-Scrolling).
     */
    public static void chainListTo(ListView<?> list, ScrollPane outer) {
        list.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) {
                return;
            }
            boolean wantsUp = event.getDeltaY() > 0;
            boolean atEdge = atScrollEdge(list, wantsUp);
            if (!atEdge) {
                return;   // Liste scrollt normal
            }
            scrollOuter(outer, event.getDeltaY(), event.isControlDown() ? 3 : 1);
            event.consume();
        });
    }

    private static boolean atScrollEdge(ListView<?> list, boolean up) {
        var bar = list.lookupAll(".scroll-bar").stream()
                .filter(node -> node instanceof javafx.scene.control.ScrollBar)
                .map(node -> (javafx.scene.control.ScrollBar) node)
                .filter(scrollBar -> scrollBar.getOrientation() == javafx.geometry.Orientation.VERTICAL)
                .findFirst();
        if (bar.isEmpty()) {
            return true;   // keine Scrollbar = Liste passt ganz rein
        }
        javafx.scene.control.ScrollBar scrollBar = bar.get();
        if (up) {
            return scrollBar.getValue() <= 0.0001;
        }
        return scrollBar.getValue() >= scrollBar.getMax() - 0.0001;
    }

    public static void scrollOuter(ScrollPane outer, double deltaY, int multiplier) {
        Node content = outer.getContent();
        if (content == null) {
            return;
        }
        double contentHeight = content.getBoundsInParent().getHeight();
        double viewportHeight = outer.getViewportBounds().getHeight();
        double scrollable = contentHeight - viewportHeight;
        if (scrollable <= 0) {
            return;
        }
        double deltaPixels = -deltaY * multiplier;
        double target = outer.getVvalue() + deltaPixels / scrollable;
        outer.setVvalue(Math.max(0, Math.min(1, target)));
    }
}
