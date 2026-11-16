
package android.view;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.input.HostUsiVersion;
import android.hardware.input.InputDeviceIdentifier;
import android.os.NullVibrator;
import android.os.Vibrator;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
public final class InputDevice implements Parcelable {
    private final int mId;
    private final int mGeneration;
    private final int mControllerNumber;
    private final String mName;
    private final int mVendorId;
    private final int mProductId;
    private final int mDeviceBus;
    private final String mDescriptor;
    private final InputDeviceIdentifier mIdentifier;
    private final boolean mIsExternal;
    private final boolean mIsVirtualDevice;
    @Source
    private final int mSources;
    private final int mKeyboardType;
    private final KeyCharacterMap mKeyCharacterMap;
    @Nullable
    private final String mKeyboardLanguageTag;
    @Nullable
    private final String mKeyboardLayoutType;
    private final boolean mHasVibrator;
    private final boolean mHasMicrophone;
    private final boolean mHasSensor;
    private final boolean mHasBattery;
    private final HostUsiVersion mHostUsiVersion;
    private final int mAssociatedDisplayId;
    private final boolean mEnabled;
    private final ArrayList<MotionRange> mMotionRanges = new ArrayList<MotionRange>();
    private final ViewBehavior mViewBehavior = new ViewBehavior(this);
    private Vibrator mVibrator; // guarded by mMotionRanges during initialization
    public static final int SOURCE_CLASS_MASK = 0x000000ff;
    public static final int SOURCE_CLASS_NONE = 0x00000000;
    public static final int SOURCE_CLASS_BUTTON = 0x00000001;
    public static final int SOURCE_CLASS_POINTER = 0x00000002;
    public static final int SOURCE_CLASS_TRACKBALL = 0x00000004;
    public static final int SOURCE_CLASS_POSITION = 0x00000008;
    public static final int SOURCE_CLASS_JOYSTICK = 0x00000010;
    @IntDef(flag = true, prefix = { "SOURCE_CLASS_" }, value = {
            SOURCE_CLASS_NONE,
            SOURCE_CLASS_BUTTON,
            SOURCE_CLASS_POINTER,
            SOURCE_CLASS_TRACKBALL,
            SOURCE_CLASS_POSITION,
            SOURCE_CLASS_JOYSTICK
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface InputSourceClass {}
    public static final int SOURCE_UNKNOWN = 0x00000000;
    public static final int SOURCE_KEYBOARD = 0x00000100 | SOURCE_CLASS_BUTTON;
    public static final int SOURCE_DPAD = 0x00000200 | SOURCE_CLASS_BUTTON;
    public static final int SOURCE_GAMEPAD = 0x00000400 | SOURCE_CLASS_BUTTON;
    public static final int SOURCE_TOUCHSCREEN = 0x00001000 | SOURCE_CLASS_POINTER;
    public static final int SOURCE_MOUSE = 0x00002000 | SOURCE_CLASS_POINTER;
    public static final int SOURCE_STYLUS = 0x00004000 | SOURCE_CLASS_POINTER;
    public static final int SOURCE_BLUETOOTH_STYLUS =
            0x00008000 | SOURCE_STYLUS;
    public static final int SOURCE_TRACKBALL = 0x00010000 | SOURCE_CLASS_TRACKBALL;
    public static final int SOURCE_MOUSE_RELATIVE = 0x00020000 | SOURCE_CLASS_TRACKBALL;
    public static final int SOURCE_TOUCHPAD = 0x00100000 | SOURCE_CLASS_POSITION;
    public static final int SOURCE_TOUCH_NAVIGATION = 0x00200000 | SOURCE_CLASS_NONE;
    public static final int SOURCE_ROTARY_ENCODER = 0x00400000 | SOURCE_CLASS_NONE;
    public static final int SOURCE_JOYSTICK = 0x01000000 | SOURCE_CLASS_JOYSTICK;
    public static final int SOURCE_HDMI = 0x02000000 | SOURCE_CLASS_BUTTON;
    public static final int SOURCE_SENSOR = 0x04000000 | SOURCE_CLASS_NONE;
    public static final int SOURCE_ANY = 0xffffff00;
    @IntDef(flag = true, prefix = { "SOURCE_" }, value = {
            SOURCE_UNKNOWN,
            SOURCE_KEYBOARD,
            SOURCE_DPAD,
            SOURCE_GAMEPAD,
            SOURCE_TOUCHSCREEN,
            SOURCE_MOUSE,
            SOURCE_STYLUS,
            SOURCE_BLUETOOTH_STYLUS,
            SOURCE_TRACKBALL,
            SOURCE_MOUSE_RELATIVE,
            SOURCE_TOUCHPAD,
            SOURCE_TOUCH_NAVIGATION,
            SOURCE_ROTARY_ENCODER,
            SOURCE_JOYSTICK,
            SOURCE_HDMI,
            SOURCE_SENSOR,
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface Source {}
    @Deprecated
    public static final int MOTION_RANGE_X = MotionEvent.AXIS_X;
    @Deprecated
    public static final int MOTION_RANGE_Y = MotionEvent.AXIS_Y;
    @Deprecated
    public static final int MOTION_RANGE_PRESSURE = MotionEvent.AXIS_PRESSURE;
    @Deprecated
    public static final int MOTION_RANGE_SIZE = MotionEvent.AXIS_SIZE;
    @Deprecated
    public static final int MOTION_RANGE_TOUCH_MAJOR = MotionEvent.AXIS_TOUCH_MAJOR;
    @Deprecated
    public static final int MOTION_RANGE_TOUCH_MINOR = MotionEvent.AXIS_TOUCH_MINOR;
    @Deprecated
    public static final int MOTION_RANGE_TOOL_MAJOR = MotionEvent.AXIS_TOOL_MAJOR;
    @Deprecated
    public static final int MOTION_RANGE_TOOL_MINOR = MotionEvent.AXIS_TOOL_MINOR;
    @Deprecated
    public static final int MOTION_RANGE_ORIENTATION = MotionEvent.AXIS_ORIENTATION;
    public static final int KEYBOARD_TYPE_NONE = 0;
    public static final int KEYBOARD_TYPE_NON_ALPHABETIC = 1;
    public static final int KEYBOARD_TYPE_ALPHABETIC = 2;
    private static final int MAX_RANGES = 1000;
    private static final int VIBRATOR_ID_ALL = -1;
    public static final @android.annotation.NonNull Parcelable.Creator<InputDevice> CREATOR =
            new Parcelable.Creator<InputDevice>() {
        public InputDevice createFromParcel(Parcel in) {
            return new InputDevice(in);
        }
        public InputDevice[] newArray(int size) {
            return new InputDevice[size];
        }
    };
    private InputDevice(int id, int generation, int controllerNumber, String name, int vendorId,
            int productId, int deviceBus, String descriptor, boolean isExternal,
            boolean isVirtualDevice, int sources, int keyboardType, KeyCharacterMap keyCharacterMap,
            @Nullable String keyboardLanguageTag, @Nullable String keyboardLayoutType,
            boolean hasVibrator, boolean hasMicrophone, boolean hasSensor, boolean hasBattery,
            int usiVersionMajor, int usiVersionMinor, int associatedDisplayId, boolean enabled) {
        mId = id;
        mGeneration = generation;
        mControllerNumber = controllerNumber;
        mName = name;
        mVendorId = vendorId;
        mProductId = productId;
        mDeviceBus = deviceBus;
        mDescriptor = descriptor;
        mIsExternal = isExternal;
        mIsVirtualDevice = isVirtualDevice;
        mSources = sources;
        mKeyboardType = keyboardType;
        mKeyCharacterMap = keyCharacterMap;
        if (keyboardLanguageTag != null && !keyboardLanguageTag.isEmpty()) {
            /* AOSP canonicalizes the tag with ICU's ULocale; ATL keeps the BCP 47 tag of java.util.Locale */
            String langTag = java.util.Locale.forLanguageTag(keyboardLanguageTag).toLanguageTag();
            mKeyboardLanguageTag = "und".equals(langTag) ? null : langTag;
        } else {
            mKeyboardLanguageTag = null;
        }
        mKeyboardLayoutType = keyboardLayoutType;
        mHasVibrator = hasVibrator;
        mHasMicrophone = hasMicrophone;
        mHasSensor = hasSensor;
        mHasBattery = hasBattery;
        mIdentifier = new InputDeviceIdentifier(descriptor, vendorId, productId);
        mHostUsiVersion = new HostUsiVersion(usiVersionMajor, usiVersionMinor);
        mAssociatedDisplayId = associatedDisplayId;
        mEnabled = enabled;
    }
    private InputDevice(Parcel in) {
        mKeyCharacterMap = KeyCharacterMap.load(0);
        mId = in.readInt();
        mGeneration = in.readInt();
        mControllerNumber = in.readInt();
        mName = in.readString();
        mVendorId = in.readInt();
        mProductId = in.readInt();
        mDeviceBus = in.readInt();
        mDescriptor = in.readString();
        mIsExternal = in.readInt() != 0;
        mIsVirtualDevice = in.readInt() != 0;
        mSources = in.readInt();
        mKeyboardType = in.readInt();
        mKeyboardLanguageTag = in.readString8();
        mKeyboardLayoutType = in.readString8();
        mHasVibrator = in.readInt() != 0;
        mHasMicrophone = in.readInt() != 0;
        mHasSensor = in.readInt() != 0;
        mHasBattery = in.readInt() != 0;
        mHostUsiVersion = HostUsiVersion.CREATOR.createFromParcel(in);
        mAssociatedDisplayId = in.readInt();
        mEnabled = in.readInt() != 0;
        mIdentifier = new InputDeviceIdentifier(mDescriptor, mVendorId, mProductId);
        int numRanges = in.readInt();
        if (numRanges > MAX_RANGES) {
            numRanges = MAX_RANGES;
        }
        for (int i = 0; i < numRanges; i++) {
            addMotionRange(in.readInt(), in.readInt(), in.readFloat(), in.readFloat(),
                    in.readFloat(), in.readFloat(), in.readFloat());
        }
        mViewBehavior.mShouldSmoothScroll = in.readBoolean();
    }
    public static class Builder {
        private int mId = 0;
        private int mGeneration = 0;
        private int mControllerNumber = 0;
        private String mName = "";
        private int mVendorId = 0;
        private int mProductId = 0;
        private int mDeviceBus = 0;
        private String mDescriptor = "";
        private boolean mIsExternal = false;
        private boolean mIsVirtualDevice = false;
        private int mSources = 0;
        private int mKeyboardType = 0;
        private KeyCharacterMap mKeyCharacterMap = null;
        private boolean mHasVibrator = false;
        private boolean mHasMicrophone = false;
        private boolean mHasSensor = false;
        private boolean mHasBattery = false;
        private String mKeyboardLanguageTag = null;
        private String mKeyboardLayoutType = null;
        private int mUsiVersionMajor = -1;
        private int mUsiVersionMinor = -1;
        private int mAssociatedDisplayId = Display.INVALID_DISPLAY;
        private boolean mEnabled = true;
        private List<MotionRange> mMotionRanges = new ArrayList<>();
        private boolean mShouldSmoothScroll;
        public Builder setId(int id) {
            mId = id;
            return this;
        }
        public Builder setGeneration(int generation) {
            mGeneration = generation;
            return this;
        }
        public Builder setControllerNumber(int controllerNumber) {
            mControllerNumber = controllerNumber;
            return this;
        }
        public Builder setName(String name) {
            mName = name;
            return this;
        }
        public Builder setVendorId(int vendorId) {
            mVendorId = vendorId;
            return this;
        }
        public Builder setProductId(int productId) {
            mProductId = productId;
            return this;
        }
        public Builder setDeviceBus(int deviceBus) {
            mDeviceBus = deviceBus;
            return this;
        }
        public Builder setDescriptor(String descriptor) {
            mDescriptor = descriptor;
            return this;
        }
        public Builder setExternal(boolean external) {
            mIsExternal = external;
            return this;
        }
        public Builder setIsVirtualDevice(boolean isVirtualDevice) {
            mIsVirtualDevice = isVirtualDevice;
            return this;
        }
        public Builder setSources(int sources) {
            mSources = sources;
            return this;
        }
        public Builder setKeyboardType(int keyboardType) {
            mKeyboardType = keyboardType;
            return this;
        }
        public Builder setKeyCharacterMap(KeyCharacterMap keyCharacterMap) {
            mKeyCharacterMap = keyCharacterMap;
            return this;
        }
        public Builder setHasVibrator(boolean hasVibrator) {
            mHasVibrator = hasVibrator;
            return this;
        }
        public Builder setHasMicrophone(boolean hasMicrophone) {
            mHasMicrophone = hasMicrophone;
            return this;
        }
        public Builder setHasSensor(boolean hasSensor) {
            mHasSensor = hasSensor;
            return this;
        }
        public Builder setHasBattery(boolean hasBattery) {
            mHasBattery = hasBattery;
            return this;
        }
        public Builder setKeyboardLanguageTag(String keyboardLanguageTag) {
            mKeyboardLanguageTag = keyboardLanguageTag;
            return this;
        }
        public Builder setKeyboardLayoutType(String keyboardLayoutType) {
            mKeyboardLayoutType = keyboardLayoutType;
            return this;
        }
        public Builder setUsiVersion(@Nullable HostUsiVersion usiVersion) {
            mUsiVersionMajor = usiVersion != null ? usiVersion.getMajorVersion() : -1;
            mUsiVersionMinor = usiVersion != null ? usiVersion.getMinorVersion() : -1;
            return this;
        }
        public Builder setAssociatedDisplayId(int displayId) {
            mAssociatedDisplayId = displayId;
            return this;
        }
        public Builder setEnabled(boolean enabled) {
            mEnabled = enabled;
            return this;
        }
        public Builder addMotionRange(int axis, int source,
                float min, float max, float flat, float fuzz, float resolution) {
            mMotionRanges.add(new MotionRange(axis, source, min, max, flat, fuzz, resolution));
            return this;
        }
        public Builder setShouldSmoothScroll(boolean shouldSmoothScroll) {
            mShouldSmoothScroll = shouldSmoothScroll;
            return this;
        }
        public InputDevice build() {
            InputDevice device = new InputDevice(
                    mId,
                    mGeneration,
                    mControllerNumber,
                    mName,
                    mVendorId,
                    mProductId,
                    mDeviceBus,
                    mDescriptor,
                    mIsExternal,
                    mIsVirtualDevice,
                    mSources,
                    mKeyboardType,
                    mKeyCharacterMap,
                    mKeyboardLanguageTag,
                    mKeyboardLayoutType,
                    mHasVibrator,
                    mHasMicrophone,
                    mHasSensor,
                    mHasBattery,
                    mUsiVersionMajor,
                    mUsiVersionMinor,
                    mAssociatedDisplayId,
                    mEnabled);
            final int numRanges = mMotionRanges.size();
            for (int i = 0; i < numRanges; i++) {
                final MotionRange range = mMotionRanges.get(i);
                device.addMotionRange(
                        range.getAxis(),
                        range.getSource(),
                        range.getMin(),
                        range.getMax(),
                        range.getFlat(),
                        range.getFuzz(),
                        range.getResolution());
            }
            device.setShouldSmoothScroll(mShouldSmoothScroll);
            return device;
        }
    }
    /* ATL has one input device (the GDK seat: touchscreen, mouse and keyboard in one); its events carry
     * other device ids than the id of the device (MotionEvent.getDeviceId()), so every id gets it */
    private static final InputDevice sDevice = createDevice();

    private static InputDevice createDevice() {
        Builder builder = new Builder()
                .setId(0)
                .setName("ATL input device")
                .setDescriptor("atl-input-device")
                .setSources(SOURCE_TOUCHSCREEN | SOURCE_MOUSE)
                .setKeyboardType(KEYBOARD_TYPE_ALPHABETIC)
                .setKeyCharacterMap(KeyCharacterMap.load(0));
        /* the ranges of the axes (AXIS_X ... AXIS_GENERIC_16 are 0-31), ATL reports no real minima and maxima */
        for (int axis = 0; axis < 32; axis++) {
            builder.addMotionRange(axis, SOURCE_TOUCHSCREEN, 0, 0, 0, 0, 0);
            builder.addMotionRange(axis, SOURCE_MOUSE, 0, 0, 0, 0, 0);
        }
        return builder.build();
    }

    @Nullable
    public static InputDevice getDevice(int id) {
        return sDevice;
    }

    public static int[] getDeviceIds() {
        return new int[] {sDevice.getId()};
    }

    public int getId() {
        return mId;
    }
    public int getControllerNumber() {
        return mControllerNumber;
    }
    @NonNull
    public InputDeviceIdentifier getIdentifier() {
        return mIdentifier;
    }
    public int getGeneration() {
        return mGeneration;
    }
    public int getVendorId() {
        return mVendorId;
    }
    public int getProductId() {
        return mProductId;
    }
    public int getDeviceBus() {
        return mDeviceBus;
    }
    public String getDescriptor() {
        return mDescriptor;
    }
    public boolean isVirtual() {
        return mId < 0;
    }
    public boolean isExternal() {
        return mIsExternal;
    }
    public boolean isPhysicalDevice() {
        return !mIsVirtualDevice && !isVirtual();
    }
    public boolean isFullKeyboard() {
        return (mSources & SOURCE_KEYBOARD) == SOURCE_KEYBOARD
                && mKeyboardType == KEYBOARD_TYPE_ALPHABETIC;
    }
    public String getName() {
        return mName;
    }
    public int getSources() {
        return mSources;
    }
    public boolean supportsSource(int source) {
        return (mSources & source) == source;
    }
    public int getKeyboardType() {
        return mKeyboardType;
    }
    public KeyCharacterMap getKeyCharacterMap() {
        return mKeyCharacterMap;
    }
    @Nullable
    public String getKeyboardLanguageTag() {
        return mKeyboardLanguageTag;
    }
    @Nullable
    public String getKeyboardLayoutType() {
        return mKeyboardLayoutType;
    }
    public boolean[] hasKeys(int... keys) {
        /* ATL pretends the device has every key */
        boolean[] ret = new boolean[keys.length];
        java.util.Arrays.fill(ret, true);
        return ret;
    }
    public int getKeyCodeForKeyLocation(int locationKeyCode) {
        /* the key layout of ATL's keyboard is the one of the location (US QWERTY) */
        return locationKeyCode;
    }
    public MotionRange getMotionRange(int axis) {
        final int numRanges = mMotionRanges.size();
        for (int i = 0; i < numRanges; i++) {
            final MotionRange range = mMotionRanges.get(i);
            if (range.mAxis == axis) {
                return range;
            }
        }
        return null;
    }
    public MotionRange getMotionRange(int axis, int source) {
        final int numRanges = mMotionRanges.size();
        for (int i = 0; i < numRanges; i++) {
            final MotionRange range = mMotionRanges.get(i);
            if (range.mAxis == axis && range.mSource == source) {
                return range;
            }
        }
        return null;
    }
    public List<MotionRange> getMotionRanges() {
        return mMotionRanges;
    }
    @NonNull
    public ViewBehavior getViewBehavior() {
        return mViewBehavior;
    }
    private void addMotionRange(int axis, int source,
            float min, float max, float flat, float fuzz, float resolution) {
        mMotionRanges.add(new MotionRange(axis, source, min, max, flat, fuzz, resolution));
    }
    private void setShouldSmoothScroll(boolean shouldSmoothScroll) {
        mViewBehavior.mShouldSmoothScroll = shouldSmoothScroll;
    }
    @Nullable
    public String getBluetoothAddress() {
        return null; // not a Bluetooth device
    }
    @Deprecated
    public Vibrator getVibrator() {
        synchronized (mMotionRanges) {
            if (mVibrator == null) {
                /* ATL's input device has no vibrator, so AOSP's NullVibrator */
                mVibrator = new NullVibrator();
            }
            return mVibrator;
        }
    }

    public boolean isEnabled() {
        return mEnabled;
    }
    public boolean hasMicrophone() {
        return mHasMicrophone;
    }
    public boolean hasSensor() {
        return mHasSensor;
    }
    public boolean hasBattery() {
        return mHasBattery;
    }
    @Nullable
    public HostUsiVersion getHostUsiVersion() {
        return mHostUsiVersion.isValid() ? mHostUsiVersion : null;
    }
    public int getAssociatedDisplayId() {
        return mAssociatedDisplayId;
    }
    public static final class MotionRange {
        private int mAxis;
        private int mSource;
        private float mMin;
        private float mMax;
        private float mFlat;
        private float mFuzz;
        private float mResolution;
        private MotionRange(int axis, int source, float min, float max, float flat, float fuzz,
                float resolution) {
            mAxis = axis;
            mSource = source;
            mMin = min;
            mMax = max;
            mFlat = flat;
            mFuzz = fuzz;
            mResolution = resolution;
        }
        public int getAxis() {
            return mAxis;
        }
        public int getSource() {
            return mSource;
        }
        public boolean isFromSource(int source) {
            return (getSource() & source) == source;
        }
        public float getMin() {
            return mMin;
        }
        public float getMax() {
            return mMax;
        }
        public float getRange() {
            return mMax - mMin;
        }
        public float getFlat() {
            return mFlat;
        }
        public float getFuzz() {
            return mFuzz;
        }
        public float getResolution() {
            return mResolution;
        }
    }
    public static final class ViewBehavior {
        private static final boolean DEFAULT_SHOULD_SMOOTH_SCROLL = false;
        private final InputDevice mInputDevice;
        private boolean mShouldSmoothScroll = DEFAULT_SHOULD_SMOOTH_SCROLL;
        public ViewBehavior(@NonNull InputDevice inputDevice) {
            mInputDevice = inputDevice;
        }
        public boolean shouldSmoothScroll(int axis, int source) {
            if (mInputDevice.getMotionRange(axis, source) == null) {
                return false;
            }
            return mShouldSmoothScroll;
        }
    }
    @Override
    public void writeToParcel(Parcel out, int flags) {
        /* ATL's KeyCharacterMap is not Parcelable, the parcel of AOSP starts with it */
        out.writeInt(mId);
        out.writeInt(mGeneration);
        out.writeInt(mControllerNumber);
        out.writeString(mName);
        out.writeInt(mVendorId);
        out.writeInt(mProductId);
        out.writeInt(mDeviceBus);
        out.writeString(mDescriptor);
        out.writeInt(mIsExternal ? 1 : 0);
        out.writeInt(mIsVirtualDevice ? 1 : 0);
        out.writeInt(mSources);
        out.writeInt(mKeyboardType);
        out.writeString8(mKeyboardLanguageTag);
        out.writeString8(mKeyboardLayoutType);
        out.writeInt(mHasVibrator ? 1 : 0);
        out.writeInt(mHasMicrophone ? 1 : 0);
        out.writeInt(mHasSensor ? 1 : 0);
        out.writeInt(mHasBattery ? 1 : 0);
        mHostUsiVersion.writeToParcel(out, flags);
        out.writeInt(mAssociatedDisplayId);
        out.writeInt(mEnabled ? 1 : 0);
        int numRanges = mMotionRanges.size();
        numRanges = numRanges > MAX_RANGES ? MAX_RANGES : numRanges;
        out.writeInt(numRanges);
        for (int i = 0; i < numRanges; i++) {
            MotionRange range = mMotionRanges.get(i);
            out.writeInt(range.mAxis);
            out.writeInt(range.mSource);
            out.writeFloat(range.mMin);
            out.writeFloat(range.mMax);
            out.writeFloat(range.mFlat);
            out.writeFloat(range.mFuzz);
            out.writeFloat(range.mResolution);
        }
        out.writeBoolean(mViewBehavior.mShouldSmoothScroll);
    }
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public String toString() {
        StringBuilder description = new StringBuilder();
        description.append("Input Device ").append(mId).append(": ").append(mName).append("\n");
        description.append("  Descriptor: ").append(mDescriptor).append("\n");
        description.append("  Generation: ").append(mGeneration).append("\n");
        description.append("  Location: ").append(mIsExternal ? "external" : "built-in").append(
                "\n");
        description.append("  Virtual: ").append(mIsVirtualDevice).append("\n");
        description.append("  Enabled: ").append(isEnabled()).append("\n");
        description.append("  Keyboard Type: ");
        switch (mKeyboardType) {
            case KEYBOARD_TYPE_NONE:
                description.append("none");
                break;
            case KEYBOARD_TYPE_NON_ALPHABETIC:
                description.append("non-alphabetic");
                break;
            case KEYBOARD_TYPE_ALPHABETIC:
                description.append("alphabetic");
                break;
        }
        description.append("\n");
        description.append("  Has Vibrator: ").append(mHasVibrator).append("\n");
        description.append("  Has Sensor: ").append(mHasSensor).append("\n");
        description.append("  Has battery: ").append(mHasBattery).append("\n");
        description.append("  Has mic: ").append(mHasMicrophone).append("\n");
        description.append("  USI Version: ").append(getHostUsiVersion()).append("\n");
        if (mKeyboardLanguageTag != null) {
            description.append(" Keyboard language tag: ").append(mKeyboardLanguageTag).append(
                    "\n");
        }
        if (mKeyboardLayoutType != null) {
            description.append(" Keyboard layout type: ").append(mKeyboardLayoutType).append("\n");
        }
        description.append("  Sources: 0x").append(Integer.toHexString(mSources)).append(" (");
        appendSourceDescriptionIfApplicable(description, SOURCE_KEYBOARD, "keyboard");
        appendSourceDescriptionIfApplicable(description, SOURCE_DPAD, "dpad");
        appendSourceDescriptionIfApplicable(description, SOURCE_TOUCHSCREEN, "touchscreen");
        appendSourceDescriptionIfApplicable(description, SOURCE_MOUSE, "mouse");
        appendSourceDescriptionIfApplicable(description, SOURCE_STYLUS, "stylus");
        appendSourceDescriptionIfApplicable(description, SOURCE_TRACKBALL, "trackball");
        appendSourceDescriptionIfApplicable(description, SOURCE_MOUSE_RELATIVE, "mouse_relative");
        appendSourceDescriptionIfApplicable(description, SOURCE_TOUCHPAD, "touchpad");
        appendSourceDescriptionIfApplicable(description, SOURCE_JOYSTICK, "joystick");
        appendSourceDescriptionIfApplicable(description, SOURCE_GAMEPAD, "gamepad");
        description.append(" )\n");
        final int numAxes = mMotionRanges.size();
        for (int i = 0; i < numAxes; i++) {
            MotionRange range = mMotionRanges.get(i);
            description.append("    ").append(MotionEvent.axisToString(range.mAxis));
            description.append(": source=0x").append(Integer.toHexString(range.mSource));
            description.append(" min=").append(range.mMin);
            description.append(" max=").append(range.mMax);
            description.append(" flat=").append(range.mFlat);
            description.append(" fuzz=").append(range.mFuzz);
            description.append(" resolution=").append(range.mResolution);
            description.append("\n");
        }
        return description.toString();
    }
    private void appendSourceDescriptionIfApplicable(StringBuilder description, int source,
            String sourceName) {
        if ((mSources & source) == source) {
            description.append(" ");
            description.append(sourceName);
        }
    }
}
