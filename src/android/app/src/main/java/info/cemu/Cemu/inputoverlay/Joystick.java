package info.cemu.Cemu.inputoverlay;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;

import java.util.Objects;

import info.cemu.Cemu.drawable.DrawableExtensions;

public class Joystick extends Input {
    private final Drawable iconPressed;
    private final Drawable iconNotPressed;
    private final Drawable joystickBackground;
    private Drawable icon;
    private int currentPointerId = -1;
    private int centerX = 0;
    private int centerY = 0;
    private int originalCenterX = 0;
    private int originalCenterY = 0;
    private int radius = 0;
    private int innerRadius = 0;

    public interface StickStateChangeListener {
        void onStickStateChange(InputOverlaySurfaceView.OverlayJoystick joystick, float x, float y);
    }

    private final StickStateChangeListener stickStateChangeListener;
    private final InputOverlaySurfaceView.OverlayJoystick joystick;

    public Joystick(Resources resources, @DrawableRes int joystickBackgroundId, @DrawableRes int innerStickId, StickStateChangeListener stickStateChangeListener, InputOverlaySurfaceView.OverlayJoystick joystick, InputOverlaySettingsProvider.InputOverlaySettings settings) {
        super(settings);
        joystickBackground = Objects.requireNonNull(ResourcesCompat.getDrawable(resources, joystickBackgroundId, null));
        iconNotPressed = Objects.requireNonNull(ResourcesCompat.getDrawable(resources, innerStickId, null));
        iconPressed = DrawableExtensions.getInvertedDrawable(iconNotPressed, resources);
        icon = iconNotPressed;
        this.stickStateChangeListener = stickStateChangeListener;
        this.joystick = joystick;
        configure();
    }

    private void updateState(boolean pressed, float x, float y) {
        if (pressed) {
            icon = iconPressed;
        } else {
            icon = iconNotPressed;
        }
        stickStateChangeListener.onStickStateChange(joystick, x, y);
        int newCentreX = (int) (centerX + radius * x);
        int newCentreY = (int) (centerY + radius * y);
        iconPressed.setBounds(newCentreX - innerRadius, newCentreY - innerRadius, newCentreX + innerRadius, newCentreY + innerRadius);
    }

    @Override
    protected void configure() {
        var joystickBounds = settings.getRect();
        this.originalCenterX = this.centerX = joystickBounds.centerX();
        this.originalCenterY = this.centerY = joystickBounds.centerY();
        this.radius = Math.min(joystickBounds.width(), joystickBounds.height()) / 2;
        this.innerRadius = (int) (radius * 0.65f);
        joystickBackground.setBounds(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        joystickBackground.setAlpha(settings.getAlpha());
        iconPressed.setAlpha(settings.getAlpha());
        iconPressed.setBounds(centerX - innerRadius, centerY - innerRadius, centerX + innerRadius, centerY + innerRadius);
        iconNotPressed.setAlpha(settings.getAlpha());
        iconNotPressed.setBounds(centerX - innerRadius, centerY - innerRadius, centerX + innerRadius, centerY + innerRadius);
    }

    @Override
    public boolean onTouch(MotionEvent event) {
        boolean stateUpdated = false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                int pointerIndex = event.getActionIndex();
                int x = (int) event.getX(pointerIndex);
                int y = (int) event.getY(pointerIndex);
                if (isInside(x, y)) {
                    centerX = x;
                    centerY = y;
                    joystickBackground.setBounds(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
                    currentPointerId = event.getPointerId(pointerIndex);
                    updateState(true, 0.0f, 0.0f);
                    stateUpdated = true;
                }
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (currentPointerId == event.getPointerId(event.getActionIndex())) {
                    centerX = originalCenterX;
                    centerY = originalCenterY;
                    joystickBackground.setBounds(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
                    currentPointerId = -1;
                    updateState(false, 0.0f, 0.0f);
                    stateUpdated = true;
                }
            }
            case MotionEvent.ACTION_MOVE -> {
                if (currentPointerId == -1) {
                    break;
                }
                for (int i = 0; i < event.getPointerCount(); i++) {
                    if (currentPointerId != event.getPointerId(i)) {
                        continue;
                    }
                    float x = (event.getX(i) - centerX) / radius;
                    float y = (event.getY(i) - centerY) / radius;
                    float norm = (float) Math.sqrt(x * x + y * y);
                    if (norm > 1.0f) {
                        x /= norm;
                        y /= norm;
                    }
                    updateState(true, x, y);
                    stateUpdated = true;
                    break;
                }
            }
        }
        return stateUpdated;
    }

    @Override
    protected void resetInput() {
        updateState(false, 0, 0);
    }

    @Override
    protected void drawInput(Canvas canvas) {
        joystickBackground.draw(canvas);
        icon.draw(canvas);
    }

    @Override
    public boolean isInside(int x, int y) {
        return ((x - originalCenterX) * (x - originalCenterX) + (y - originalCenterY) * (y - originalCenterY)) <= radius * radius;
    }

}
