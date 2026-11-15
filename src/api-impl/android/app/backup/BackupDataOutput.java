package android.app.backup;

/* AOSP BackupDataOutput writes the key/value backup stream through native code (BackupDataWriter) on a file
 * descriptor the backup system opens; its constructors are @hide/@SystemApi. ATL has no backup transport, so no
 * instance is ever handed to an app. The class is here for the type. */
public class BackupDataOutput {
	BackupDataOutput() {}
}
