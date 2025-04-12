package com.example;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**Provides the ability to move and resize the application window */
public class VBoxResizer {

    private static final int DRAGGING_MARGIN = 25;
    private boolean dragging = false;
    private Point2D dragAnchor;

    public VBoxResizer(Stage stage, VBox vbox) {
        enableResizeAndDrag(stage, vbox);
    }

    private boolean isInDraggableZone(MouseEvent event) {
        return event.getY() < DRAGGING_MARGIN && event.getY() > 4;
    }

    // Combining dragging and resizing functionality
    public void enableResizeAndDrag(Stage stage, VBox root) {
        final int border = 4;

        root.setOnMousePressed(event -> {
            if (isInDraggableZone(event)) {
                dragging = true;
                dragAnchor = new Point2D(event.getSceneX(), event.getSceneY());
            }
        });

        root.setOnMouseMoved(event -> {
            if (isInDraggableZone(event)) {
                root.setCursor(Cursor.DEFAULT);
            } else if (event.getX() < border && event.getY() < border) {
                root.setCursor(Cursor.NW_RESIZE);
            } else if (event.getX() < border && event.getY() > root.getHeight() - border) {
                root.setCursor(Cursor.SW_RESIZE);
            } else if (event.getX() > root.getWidth() - border && event.getY() < border) {
                root.setCursor(Cursor.NE_RESIZE);
            } else if (event.getX() > root.getWidth() - border && event.getY() > root.getHeight() - border) {
                root.setCursor(Cursor.SE_RESIZE);
            } else if (event.getX() < border) {
                root.setCursor(Cursor.W_RESIZE);
            } else if (event.getX() > root.getWidth() - border) {
                root.setCursor(Cursor.E_RESIZE);
            } else if (event.getY() < border) {
                root.setCursor(Cursor.N_RESIZE);
            } else if (event.getY() > root.getHeight() - border) {
                root.setCursor(Cursor.S_RESIZE);
            } else {
                root.setCursor(Cursor.DEFAULT);
            }
        });

        root.setOnMouseDragged(event -> {
            if (root.getCursor() == Cursor.DEFAULT && dragging) {
                stage.setX(event.getScreenX() - dragAnchor.getX());
                stage.setY(event.getScreenY() - dragAnchor.getY());
                return;
            }

            if (root.getCursor() == Cursor.DEFAULT)
                return;

            double mouseEventX = event.getX();
            double mouseEventY = event.getY();

            double newWidth = stage.getWidth();
            double newHeight = stage.getHeight();
            double minWidthDimensions = 700;
            double minHeightDimensions = 500;

            if (root.getCursor() == Cursor.W_RESIZE || root.getCursor() == Cursor.NW_RESIZE
                    || root.getCursor() == Cursor.SW_RESIZE) {
                newWidth = stage.getWidth() - mouseEventX;
                if (newWidth >= minWidthDimensions) {
                    stage.setX(stage.getX() + mouseEventX);
                }
            }
            if (root.getCursor() == Cursor.N_RESIZE || root.getCursor() == Cursor.NW_RESIZE
                    || root.getCursor() == Cursor.NE_RESIZE) {
                newHeight = stage.getHeight() - mouseEventY;
                if (newHeight >= minHeightDimensions) {
                    stage.setY(stage.getY() + mouseEventY);
                }
            }

            if (root.getCursor() == Cursor.E_RESIZE || root.getCursor() == Cursor.SE_RESIZE
                    || root.getCursor() == Cursor.NE_RESIZE) {
                newWidth = mouseEventX;
            }
            if (root.getCursor() == Cursor.S_RESIZE || root.getCursor() == Cursor.SE_RESIZE
                    || root.getCursor() == Cursor.SW_RESIZE) {
                newHeight = mouseEventY;
            }

            if (newWidth >= minWidthDimensions) {
                stage.setWidth(newWidth);
            }
            if (newHeight >= minHeightDimensions) {
                stage.setHeight(newHeight);
            }
        });

        root.setOnMouseReleased(event -> dragging = false);
    }

    public static void apply(Stage stage, VBox vbox) {
        new VBoxResizer(stage, vbox);
    }
}
