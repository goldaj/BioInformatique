package service.interfaces;

import java.awt.*;

public class ApiStatus {
    private String message;
    private Color color;

    public static final Color ONLINE_COLOR = new Color(0, 157, 44);
    public static final Color OFFLINE_COLOR = Color.RED;
    public static final Color TROUBLE_COLOR = Color.ORANGE;

    public Color getColor() {
        if (color == null) {
            return Color.GREEN;
        }
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
