package mikrolabs.dev.sisdistribuidos.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Toast {

    public enum Type {
        SUCCESS("toast-success"),
        ERROR("toast-error"),
        INFO("toast-info");

        private final String styleClass;

        Type(String styleClass) {
            this.styleClass = styleClass;
        }

        public String getStyleClass() {
            return styleClass;
        }
    }

    public static void show(Stage ownerStage, String message, Type type) {
        if (ownerStage == null || !ownerStage.isShowing()) return;

        Popup popup = new Popup();
        popup.setAutoFix(true);

        Label label = new Label(message);
        label.getStyleClass().addAll("toast-container", type.getStyleClass());

        popup.getContent().add(label);

        // Animações: Fade In -> Pausa visível -> Fade Out -> Fecha o popup
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), label);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition displayTime = new PauseTransition(Duration.millis(2500));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition sequence = new SequentialTransition(fadeIn, displayTime, fadeOut);
        sequence.setOnFinished(e -> popup.hide());

        // Renderiza invisível primeiro para obter as dimensões exatas e centralizar no canto inferior
        label.setOpacity(0.0);
        popup.show(ownerStage);

        double x = ownerStage.getX() + (ownerStage.getWidth() / 2.0) - (label.getWidth() / 2.0);
        double y = ownerStage.getY() + ownerStage.getHeight() - label.getHeight() - 55.0;

        popup.setX(x);
        popup.setY(y);

        sequence.play();
    }
}