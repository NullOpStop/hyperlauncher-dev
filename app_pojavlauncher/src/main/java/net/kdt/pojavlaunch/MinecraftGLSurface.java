package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.MainActivity.touchCharInput;
import static net.kdt.pojavlaunch.CallbackBridge.sendMouseButton;
import static net.kdt.pojavlaunch.CallbackBridge.windowHeight;
import static net.kdt.pojavlaunch.CallbackBridge.windowWidth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.gamepad.DefaultDataProvider;
import net.kdt.pojavlaunch.customcontrols.gamepad.Gamepad;
import net.kdt.pojavlaunch.customcontrols.gamepad.DirectGamepad;
import net.kdt.pojavlaunch.customcontrols.mouse.AndroidPointerCapture;
import net.kdt.pojavlaunch.customcontrols.mouse.InGUIEventProcessor;
import net.kdt.pojavlaunch.customcontrols.mouse.InGameEventProcessor;
import net.kdt.pojavlaunch.customcontrols.mouse.TouchEventProcessor;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.render.SurfaceProvider;
import net.kdt.pojavlaunch.render.SurfaceViewSurfaceProvider;
import net.kdt.pojavlaunch.render.TextureViewSurfaceProvider;
import net.kdt.pojavlaunch.utils.MCOptionUtils;

import fr.spse.gamepad_remapper.GamepadHandler;
import fr.spse.gamepad_remapper.RemapperManager;
import fr.spse.gamepad_remapper.RemapperView;
import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;
import git.artdeell.dnbootstrap.glfw.GrabListener;

import java.lang.ref.WeakReference;

/**
 * Class dealing with showing minecraft surface and taking inputs to dispatch them to minecraft
 */
public class MinecraftGLSurface extends View implements GrabListener, GamepadEnableHandler, SurfaceProvider.SurfaceCallback {
    /* Gamepad object for gamepad inputs, instantiated on need */
    private GamepadHandler mGamepadHandler;
    /* The RemapperView.Builder object allows you to set which buttons to remap */
    private final RemapperManager mInputManager = new RemapperManager(getContext(), new RemapperView.Builder(null)
            .remapA(true)
            .remapB(true)
            .remapX(true)
            .remapY(true)

            .remapLeftJoystick(true)
            .remapRightJoystick(true)
            .remapStart(true)
            .remapSelect(true)
            .remapLeftShoulder(true)
            .remapRightShoulder(true)
            .remapLeftTrigger(true)
            .remapRightTrigger(true)
            .remapDpad(true));

    /* Sensitivity, adjusted according to screen size */
    private final double mSensitivityFactor = (1.4 * (1080f/ Tools.getDisplayMetrics((Activity) getContext()).heightPixels));

    private SurfaceProvider mSurfaceProvider;
    private boolean mRefreshOnly = true;
    /* Surface ready listener, used by the activity to launch minecraft */
    SurfaceReadyListener mSurfaceReadyListener = null;
    final Object mSurfaceReadyListenerLock = new Object();
    /* View holding the surface, either a SurfaceView or a TextureView */
    View mSurface;

    private final InGameEventProcessor mIngameProcessor = new InGameEventProcessor(this, mSensitivityFactor);
    private final InGUIEventProcessor mInGUIProcessor = new InGUIEventProcessor(this);
    private TouchEventProcessor mCurrentTouchProcessor = mInGUIProcessor;
    private AndroidPointerCapture mPointerCapture;
    private View mTouchpad;
    private boolean mLastGrabState = false;

    private Thread mMainThread;

    public MinecraftGLSurface(Context context) {
        this(context, null);
    }

    public MinecraftGLSurface(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setFocusable(true);
        setFocusableInTouchMode(true);
        GLFW.setGamepadEnableHandler(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUpPointerCapture() {
        if(mPointerCapture != null) mPointerCapture.detach();
        mPointerCapture = new AndroidPointerCapture(mTouchpad, this);
    }

    /** Initialize the view and all its settings
     * @param isAlreadyRunning set to true to tell the view that the game is already running
     *                         (only updates the window without calling the start listener)
     * @param touchpad the optional cursor-emulating touchpad, used for touch event processing
     *                 when the cursor is not grabbed
     */
    public void start(boolean isAlreadyRunning, View touchpad) {
        mTouchpad = touchpad;
        requestFocus();
        if (Tools.isAndroid8OrHigher()) setUpPointerCapture();
        mInGUIProcessor.setAbstractTouchpad(touchpad);
        mRefreshOnly = isAlreadyRunning;

        boolean useSurfaceView = LauncherPreferences.PREF_USE_ALTERNATE_SURFACE;
        // Fix for Zink/Krypton: force TextureView if orientation bugs are expected
        if (useSurfaceView && Tools.LOCAL_RENDERER != null && 
            (Tools.LOCAL_RENDERER.contains("zink") || Tools.LOCAL_RENDERER.contains("krypton"))) {
            useSurfaceView = false;
        }

        mSurfaceProvider = useSurfaceView ? new SurfaceViewSurfaceProvider() : new TextureViewSurfaceProvider();
        mSurface = mSurfaceProvider.create(getContext(), this);

        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            parent.addView(mSurface, 0);
        } else {
            Log.e("MGLSurface", "Failed to add surface view: parent is null");
        }
    }

    /**
     * The touch event for both grabbed an non-grabbed mouse state on the touch screen
     * Does not cover the virtual mouse touchpad
     */
    @Override
    @SuppressWarnings("accessibility")
    public boolean onTouchEvent(MotionEvent e) {
        View parent = (View) getParent();
        if(parent instanceof ControlLayout && ((ControlLayout)parent).getModifiable()) return false;

        for (int i = 0; i < e.getPointerCount(); i++) {
            int toolType = e.getToolType(i);
            if(toolType == MotionEvent.TOOL_TYPE_MOUSE) {
                if(Tools.isAndroid8OrHigher() &&
                        LauncherPreferences.PREF_ENABLE_PHYSICAL_MOUSE &&
                        mPointerCapture != null) {
                    mPointerCapture.handleAutomaticCapture();

                    return true;
                }
            }else if(toolType == MotionEvent.TOOL_TYPE_STYLUS) {
                if(GLFW.isGrabbing()) return false;
                GLFW.cursorX = e.getX(i) / getWidth();
                GLFW.cursorY = e.getY(i) / getHeight();
                GLFW.sendMousePos();
                return true;
            }
        }

        if (mIngameProcessor == null || mInGUIProcessor == null) return true;
        return mCurrentTouchProcessor.processTouchEvent(e);
    }

    private void createGamepad(InputDevice inputDevice) {
        if(GLFW.gamepadButtonBuffer != null) {
            mGamepadHandler = new DirectGamepad();

            GLFW.nativeNotifyGamepadConnected();
        }else {
            mGamepadHandler = new Gamepad(inputDevice, DefaultDataProvider.INSTANCE, mTouchpad);
        }
    }

    /**
     * The event for mouse/joystick movements
     */
    @SuppressLint("NewApi")
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        int mouseCursorIndex = -1;

        if(Gamepad.isGamepadEvent(event)){
            if(mGamepadHandler == null) createGamepad(event.getDevice());

            mInputManager.handleMotionEventInput(getContext(), event, mGamepadHandler);
            return true;
        }

        for(int i = 0; i < event.getPointerCount(); i++) {
            if(event.getToolType(i) != MotionEvent.TOOL_TYPE_MOUSE && event.getToolType(i) != MotionEvent.TOOL_TYPE_STYLUS ) continue;

            mouseCursorIndex = i;
            break;
        }
        if(mouseCursorIndex == -1) return false;

        updateGrabState(GLFW.isGrabbing());

        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_MOVE:
                int w = getWidth();
                int h = getHeight();
                if (w > 0 && h > 0) {
                    GLFW.cursorX = (event.getX(mouseCursorIndex) / w);
                    GLFW.cursorY = (event.getY(mouseCursorIndex) / h);
                    GLFW.sendMousePos();
                }
                return true;
            case MotionEvent.ACTION_SCROLL:
                CallbackBridge.sendScroll(event.getAxisValue(MotionEvent.AXIS_HSCROLL), event.getAxisValue(MotionEvent.AXIS_VSCROLL));
                return true;
            case MotionEvent.ACTION_BUTTON_PRESS:
                return sendMouseButtonUnconverted(event.getActionButton(),true);
            case MotionEvent.ACTION_BUTTON_RELEASE:
                return sendMouseButtonUnconverted(event.getActionButton(),false);
            default:
                return false;
        }
    }

    /** The event for keyboard/ gamepad button inputs */
    public boolean processKeyEvent(KeyEvent event) {

        int eventKeycode = event.getKeyCode();
        if(eventKeycode == KeyEvent.KEYCODE_UNKNOWN) return true;

        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_UP && LauncherPreferences.PREF_VOLUME_UP_KEYBIND != 0) {
            GLFW.sendKeyEvent(LauncherPreferences.PREF_VOLUME_UP_KEYBIND, event.getAction() == KeyEvent.ACTION_DOWN ? 1 : 0, CallbackBridge.getCurrentMods());
            return true;
        }
        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_DOWN && LauncherPreferences.PREF_VOLUME_DOWN_KEYBIND != 0) {
            GLFW.sendKeyEvent(LauncherPreferences.PREF_VOLUME_DOWN_KEYBIND, event.getAction() == KeyEvent.ACTION_DOWN ? 1 : 0, CallbackBridge.getCurrentMods());
            return true;
        }

        if(eventKeycode == KeyEvent.KEYCODE_VOLUME_DOWN) return false;
        if(eventKeycode == KeyEvent.KEYCODE_VOLUME_UP) return false;
        if(event.getRepeatCount() != 0) return true;
        int action = event.getAction();
        if(action == KeyEvent.ACTION_MULTIPLE) return true;

        if(action == KeyEvent.ACTION_UP &&
                (event.getFlags() & KeyEvent.FLAG_CANCELED) != 0) return true;

        if((event.getFlags() & KeyEvent.FLAG_SOFT_KEYBOARD) == KeyEvent.FLAG_SOFT_KEYBOARD){
            if(eventKeycode == KeyEvent.KEYCODE_ENTER) return true;
            touchCharInput.dispatchKeyEvent(event);
            return true;
        }

        if(event.getDevice() != null
                && ( (event.getSource() & InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE
|   (event.getSource() & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE)  ){

            if(eventKeycode == KeyEvent.KEYCODE_BACK){
                sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, event.getAction() == KeyEvent.ACTION_DOWN);
                return true;
            }
        }

        if(Gamepad.isGamepadEvent(event)){
            if(mGamepadHandler == null) createGamepad(event.getDevice());

            mInputManager.handleKeyEventInput(getContext(), event, mGamepadHandler);
            return true;
        }

        CallbackBridge.setModifiers(event);
        char codepoint = action == KeyEvent.ACTION_DOWN ? (char) event.getUnicodeChar(event.getMetaState()) : 0;
        GLFW.sendRawKeyEvent(eventKeycode, action == KeyEvent.ACTION_DOWN ? 1 : 0, CallbackBridge.getCurrentMods(), codepoint);

        return (event.getFlags() & KeyEvent.FLAG_FALLBACK) == KeyEvent.FLAG_FALLBACK;
    }

    /** Convert the mouse button, then send it
     * @return Whether the event was processed
     */
    public static boolean sendMouseButtonUnconverted(int button, boolean status) {
        int glfwButton = -256;
        switch (button) {
            case MotionEvent.BUTTON_PRIMARY:
                glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT;
                break;
            case MotionEvent.BUTTON_TERTIARY:
                glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE;
                break;
            case MotionEvent.BUTTON_SECONDARY:
                glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT;
                break;
        }
        if(glfwButton == -256) return false;
        sendMouseButton(glfwButton, status);
        return true;
    }

    /** Called when the size need to be set at any point during the surface lifecycle **/
    public void refreshSize(){
        refreshSize(false);
    }

    /** Same as refreshSize, but allows you to force an immediate size update **/
    public void refreshSize(boolean immediate) {
        if(isInLayout() && !immediate) {
            post(this::refreshSize);
            return;
        }

        int currentWidth = getWidth();
        int currentHeight = getHeight();

        // Fix for stretching: Ensure we use the larger dimension as width for landscape.
        // Sometimes getWidth/getHeight return portrait values during startup rotation.
        if (currentWidth < currentHeight && currentWidth > 0) {
            int temp = currentWidth;
            currentWidth = currentHeight;
            currentHeight = temp;
        }

        int newWidth = Tools.getDisplayFriendlyRes(currentWidth, LauncherPreferences.PREF_SCALE_FACTOR);
        int newHeight = Tools.getDisplayFriendlyRes(currentHeight, LauncherPreferences.PREF_SCALE_FACTOR);
        if (newHeight < 1 || newWidth < 1) {
            Log.e("MGLSurface", String.format("Impossible resolution : %dx%d", newWidth, newHeight));
            return;
        }
        windowWidth = newWidth;
        windowHeight = newHeight;
        if(mSurfaceProvider == null){
            Log.w("MGLSurface", "Attempt to refresh size on null surface provider");
            return;
        }
        mSurfaceProvider.updateSize();
    }

    private void realStart(Surface surface){
        // Initial size set. Request immediate refresh.
        refreshSize(true);
        
        // Fix: Set high frame rate for the surface on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                Display display = getDisplay();
                if (display != null && surface != null && surface.isValid()) {
                    float maxHz = 60f;
                    for (Display.Mode mode : display.getSupportedModes()) {
                        if (mode.getRefreshRate() > maxHz) maxHz = mode.getRefreshRate();
                    }
                    surface.setFrameRate(maxHz, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT, Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS);
                }
            } catch (Throwable ignored) {}
        }

        MCOptionUtils.set("fullscreen", "off");
        MCOptionUtils.set("overrideWidth", String.valueOf(windowWidth));
        MCOptionUtils.set("overrideHeight", String.valueOf(windowHeight));
        MCOptionUtils.save();
        MCOptionUtils.getMcScale();

        mMainThread = new Thread(new MainThreadRunnable(this), "JVM Main thread");
        mMainThread.start();
    }

    private static class MainThreadRunnable implements Runnable {
        private final WeakReference<MinecraftGLSurface> mSurfaceRef;

        MainThreadRunnable(MinecraftGLSurface surface) {
            mSurfaceRef = new WeakReference<>(surface);
        }

        @Override
        public void run() {
            try {
                MinecraftGLSurface surface = mSurfaceRef.get();
                if (surface == null) return;

                synchronized(surface.mSurfaceReadyListenerLock) {
                    if(surface.mSurfaceReadyListener == null) surface.mSurfaceReadyListenerLock.wait();
                }

                surface = mSurfaceRef.get();
                if (surface != null && surface.mSurfaceReadyListener != null) {
                    surface.mSurfaceReadyListener.isReady();
                }
            } catch (Throwable e) {
                MinecraftGLSurface surface = mSurfaceRef.get();
                if (surface != null) {
                    Tools.showError(surface.getContext(), e, true);
                }
            }
        }
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        post(()->updateGrabState(isGrabbing));
    }

    private TouchEventProcessor pickEventProcessor(boolean isGrabbing) {
        return isGrabbing ? mIngameProcessor : mInGUIProcessor;
    }

    private void updateGrabState(boolean isGrabbing) {
        if(mLastGrabState != isGrabbing) {
            mCurrentTouchProcessor.cancelPendingActions();
            mCurrentTouchProcessor = pickEventProcessor(isGrabbing);
            mLastGrabState = isGrabbing;
        }
    }

    @Override
    public void onSurfaceAvailable(Surface surface) {
        GLFW.nativeSurfaceCreated(surface);
        if(mRefreshOnly) return;
        realStart(surface);
        mRefreshOnly = true;
    }

    @Override
    public void onSurfaceResized() {
        GLFW.nativeSurfaceUpdated();
    }

    @Override
    public void onSurfaceDestroyed() {
        GLFW.nativeSurfaceDestroyed();
    }

    @Override
    public void onEnableGamepad() {
        post(()->{
            if(mGamepadHandler != null && mGamepadHandler instanceof Gamepad) {
                ((Gamepad)mGamepadHandler).removeSelf();
            }

            mGamepadHandler = null;
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mPointerCapture != null) {
            if (Tools.isAndroid8OrHigher()) {
                mPointerCapture.detach();
            }
            mPointerCapture = null;
        }
        if (mGamepadHandler != null && mGamepadHandler instanceof Gamepad) {
            ((Gamepad) mGamepadHandler).removeSelf();
        }
        mGamepadHandler = null;

        if (mSurface != null) {
            ViewGroup parent = (ViewGroup) mSurface.getParent();
            if (parent != null) parent.removeView(mSurface);
            mSurface = null;
        }
        mSurfaceProvider = null;
        GLFW.setGamepadEnableHandler(null);
        
        if (mMainThread != null) {
            mMainThread.interrupt();
            mMainThread = null;
        }
    }

    /** A small interface called when the listener is ready for the first time */
    public interface SurfaceReadyListener {
        void isReady();
    }

    public void setSurfaceReadyListener(SurfaceReadyListener listener){
        synchronized (mSurfaceReadyListenerLock) {
            mSurfaceReadyListener = listener;
            mSurfaceReadyListenerLock.notifyAll();
        }
    }
}
