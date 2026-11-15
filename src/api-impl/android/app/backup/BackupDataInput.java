package android.app.backup;

/* AOSP BackupDataInput reads the restore stream through native code (BackupDataReader) on a file descriptor the
 * backup system opens; its constructor is @hide/@SystemApi. ATL has no backup transport, so no instance is ever
 * handed to an app. The class is here for the type. */
public class BackupDataInput {
	BackupDataInput() {}
}
