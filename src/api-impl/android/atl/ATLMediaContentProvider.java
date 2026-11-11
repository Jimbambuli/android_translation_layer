package android.atl;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* MediaStore's provider (authority "media") after AOSP 14's MediaProvider: a SQLite index of the shared storage
 * (the table files and the views of DatabaseHelper), which answers queries without any user interface. The
 * shared storage is the user's folders Pictures, Music, Videos and Download (Pictures/, Music/, Movies/ and
 * Download/ to the app; only Download is written to) and the standard directories of ATL's external storage
 * (the app data dir), where insert() puts new media. The index lives in the app data dir and is brought up to date (like
 * ModernMediaScanner) on the first use in a process. Picking a file is the file dialog of ACTION_OPEN_DOCUMENT,
 * GET_CONTENT and ACTION_PICK_IMAGES (Activity), not this provider. */
public class ATLMediaContentProvider extends ContentProvider {

	private static final String TAG = "ATLMediaProvider";

	/* the user's folders (native_user_dirs()) and their directory in the shared storage */
	private static final String[] USER_DIR_NAMES = {"Pictures", "Music", "Movies", "Download"};
	/* Environment.STANDARD_DIRECTORIES */
	private static final String[] STANDARD_DIRECTORIES = {"Music", "Podcasts", "Ringtones", "Alarms", "Notifications", "Pictures",
	                                                      "Movies", "Download", "DCIM", "Documents", "Audiobooks", "Recordings"};
	/* FileUtils.sAudioTypes */
	private static final String[][] AUDIO_TYPES = {
		{    "Ringtones",     "is_ringtone"},
		{"Notifications", "is_notification"},
		{       "Alarms",        "is_alarm"},
		{     "Podcasts",      "is_podcast"},
		{   "Audiobooks",    "is_audiobook"},
		{        "Music",        "is_music"},
		{   "Recordings",    "is_recording"}
	};

	private static final String VOLUME = MediaStore.VOLUME_EXTERNAL_PRIMARY;
	private static final int DATABASE_VERSION = 1;

	/* LocalUriMatcher */
	private static final int IMAGES_MEDIA = 1;
	private static final int IMAGES_MEDIA_ID = 2;
	private static final int IMAGES_MEDIA_ID_THUMBNAIL = 3;
	private static final int IMAGES_THUMBNAILS = 4;
	private static final int IMAGES_THUMBNAILS_ID = 5;
	private static final int AUDIO_MEDIA = 100;
	private static final int AUDIO_MEDIA_ID = 101;
	private static final int AUDIO_MEDIA_ID_GENRES = 102;
	private static final int AUDIO_MEDIA_ID_GENRES_ID = 103;
	private static final int AUDIO_GENRES = 106;
	private static final int AUDIO_GENRES_ID = 107;
	private static final int AUDIO_GENRES_ID_MEMBERS = 108;
	private static final int AUDIO_GENRES_ALL_MEMBERS = 109;
	private static final int AUDIO_PLAYLISTS = 110;
	private static final int AUDIO_PLAYLISTS_ID = 111;
	private static final int AUDIO_PLAYLISTS_ID_MEMBERS = 112;
	private static final int AUDIO_PLAYLISTS_ID_MEMBERS_ID = 113;
	private static final int AUDIO_ARTISTS = 114;
	private static final int AUDIO_ARTISTS_ID = 115;
	private static final int AUDIO_ALBUMS = 116;
	private static final int AUDIO_ALBUMS_ID = 117;
	private static final int AUDIO_ARTISTS_ID_ALBUMS = 118;
	private static final int AUDIO_ALBUMART = 119;
	private static final int AUDIO_ALBUMART_ID = 120;
	private static final int AUDIO_ALBUMART_FILE_ID = 121;
	private static final int VIDEO_MEDIA = 200;
	private static final int VIDEO_MEDIA_ID = 201;
	private static final int VIDEO_MEDIA_ID_THUMBNAIL = 202;
	private static final int VIDEO_THUMBNAILS = 203;
	private static final int VIDEO_THUMBNAILS_ID = 204;
	private static final int MEDIA_SCANNER = 500;
	private static final int FS_ID = 600;
	private static final int VERSION = 601;
	private static final int FILES = 700;
	private static final int FILES_ID = 701;
	private static final int DOWNLOADS = 800;
	private static final int DOWNLOADS_ID = 801;

	private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		final String auth = MediaStore.AUTHORITY;
		MATCHER.addURI(auth, "*/images/media", IMAGES_MEDIA);
		MATCHER.addURI(auth, "*/images/media/#", IMAGES_MEDIA_ID);
		MATCHER.addURI(auth, "*/images/media/#/thumbnail", IMAGES_MEDIA_ID_THUMBNAIL);
		MATCHER.addURI(auth, "*/images/thumbnails", IMAGES_THUMBNAILS);
		MATCHER.addURI(auth, "*/images/thumbnails/#", IMAGES_THUMBNAILS_ID);
		MATCHER.addURI(auth, "*/audio/media", AUDIO_MEDIA);
		MATCHER.addURI(auth, "*/audio/media/#", AUDIO_MEDIA_ID);
		MATCHER.addURI(auth, "*/audio/media/#/genres", AUDIO_MEDIA_ID_GENRES);
		MATCHER.addURI(auth, "*/audio/media/#/genres/#", AUDIO_MEDIA_ID_GENRES_ID);
		MATCHER.addURI(auth, "*/audio/genres", AUDIO_GENRES);
		MATCHER.addURI(auth, "*/audio/genres/#", AUDIO_GENRES_ID);
		MATCHER.addURI(auth, "*/audio/genres/#/members", AUDIO_GENRES_ID_MEMBERS);
		MATCHER.addURI(auth, "*/audio/genres/all/members", AUDIO_GENRES_ALL_MEMBERS);
		MATCHER.addURI(auth, "*/audio/playlists", AUDIO_PLAYLISTS);
		MATCHER.addURI(auth, "*/audio/playlists/#", AUDIO_PLAYLISTS_ID);
		MATCHER.addURI(auth, "*/audio/playlists/#/members", AUDIO_PLAYLISTS_ID_MEMBERS);
		MATCHER.addURI(auth, "*/audio/playlists/#/members/#", AUDIO_PLAYLISTS_ID_MEMBERS_ID);
		MATCHER.addURI(auth, "*/audio/artists", AUDIO_ARTISTS);
		MATCHER.addURI(auth, "*/audio/artists/#", AUDIO_ARTISTS_ID);
		MATCHER.addURI(auth, "*/audio/artists/#/albums", AUDIO_ARTISTS_ID_ALBUMS);
		MATCHER.addURI(auth, "*/audio/albums", AUDIO_ALBUMS);
		MATCHER.addURI(auth, "*/audio/albums/#", AUDIO_ALBUMS_ID);
		MATCHER.addURI(auth, "*/audio/albumart", AUDIO_ALBUMART);
		MATCHER.addURI(auth, "*/audio/albumart/#", AUDIO_ALBUMART_ID);
		MATCHER.addURI(auth, "*/audio/media/#/albumart", AUDIO_ALBUMART_FILE_ID);
		MATCHER.addURI(auth, "*/video/media", VIDEO_MEDIA);
		MATCHER.addURI(auth, "*/video/media/#", VIDEO_MEDIA_ID);
		MATCHER.addURI(auth, "*/video/media/#/thumbnail", VIDEO_MEDIA_ID_THUMBNAIL);
		MATCHER.addURI(auth, "*/video/thumbnails", VIDEO_THUMBNAILS);
		MATCHER.addURI(auth, "*/video/thumbnails/#", VIDEO_THUMBNAILS_ID);
		MATCHER.addURI(auth, "*/media_scanner", MEDIA_SCANNER);
		MATCHER.addURI(auth, "*/fs_id", FS_ID);
		MATCHER.addURI(auth, "*/version", VERSION);
		MATCHER.addURI(auth, "*/file", FILES);
		MATCHER.addURI(auth, "*/file/#", FILES_ID);
		MATCHER.addURI(auth, "*/downloads", DOWNLOADS);
		MATCHER.addURI(auth, "*/downloads/#", DOWNLOADS_ID);
	}

	private SQLiteDatabase db;

	private static String packageName() {
		return ATLLoadedApp.getPrimaryApplication().getApplication().getPackageName();
	}

	private synchronized SQLiteDatabase getDatabase() {
		if (db == null) {
			File dir = new File(ATLLoadedApp.getPrimaryApplication().getApplication().getDataDir(), "atl_media");
			dir.mkdirs();
			db = SQLiteDatabase.openOrCreateDatabase(new File(dir, "external.db"), null);
			if (db.getVersion() != DATABASE_VERSION) {
				createSchema(db);
				db.setVersion(DATABASE_VERSION);
			}
			long start = System.currentTimeMillis();
			scan(db);
			Log.i(TAG, "scanned the shared storage in " + (System.currentTimeMillis() - start) + " ms");
		}
		return db;
	}

	/* DatabaseHelper.createLatestSchema() and createLatestViews() for the external volume; the views of the media
	 * types have all columns of files */
	private static void createSchema(SQLiteDatabase db) {
		for (String table : new String[] {"files", "thumbnails", "videothumbnails", "album_art", "audio_playlists_map"})
			db.execSQL("DROP TABLE IF EXISTS " + table);
		for (String view : new String[] {"audio", "video", "images", "downloads", "audio_playlists", "audio_artists",
		                                 "audio_artists_albums", "audio_albums", "audio_genres"})
			db.execSQL("DROP VIEW IF EXISTS " + view);
		db.execSQL("CREATE TABLE thumbnails (_id INTEGER PRIMARY KEY,_data TEXT,image_id INTEGER,"
		           + "kind INTEGER,width INTEGER,height INTEGER)");
		db.execSQL("CREATE TABLE album_art (album_id INTEGER PRIMARY KEY,_data TEXT)");
		db.execSQL("CREATE TABLE videothumbnails (_id INTEGER PRIMARY KEY,_data TEXT,"
		           + "video_id INTEGER,kind INTEGER,width INTEGER,height INTEGER)");
		db.execSQL("CREATE TABLE files (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
		           + "_data TEXT UNIQUE COLLATE NOCASE,_size INTEGER,format INTEGER,parent INTEGER,"
		           + "date_added INTEGER,date_modified INTEGER,mime_type TEXT,title TEXT,"
		           + "description TEXT,_display_name TEXT,picasa_id TEXT,orientation INTEGER,"
		           + "latitude DOUBLE,longitude DOUBLE,datetaken INTEGER,mini_thumb_magic INTEGER,"
		           + "bucket_id TEXT,bucket_display_name TEXT,isprivate INTEGER,title_key TEXT,"
		           + "artist_id INTEGER,album_id INTEGER,composer TEXT,track INTEGER,"
		           + "year INTEGER CHECK(year!=0),is_ringtone INTEGER,is_music INTEGER,"
		           + "is_alarm INTEGER,is_notification INTEGER,is_podcast INTEGER,album_artist TEXT,"
		           + "duration INTEGER,bookmark INTEGER,artist TEXT,album TEXT,resolution TEXT,"
		           + "tags TEXT,category TEXT,language TEXT,mini_thumb_data TEXT,name TEXT,"
		           + "media_type INTEGER,old_id INTEGER,is_drm INTEGER,"
		           + "width INTEGER, height INTEGER, title_resource_uri TEXT,"
		           + "owner_package_name TEXT DEFAULT NULL,"
		           + "color_standard INTEGER, color_transfer INTEGER, color_range INTEGER,"
		           + "_hash BLOB DEFAULT NULL, is_pending INTEGER DEFAULT 0,"
		           + "is_download INTEGER DEFAULT 0, download_uri TEXT DEFAULT NULL,"
		           + "referer_uri TEXT DEFAULT NULL, is_audiobook INTEGER DEFAULT 0,"
		           + "date_expires INTEGER DEFAULT NULL,is_trashed INTEGER DEFAULT 0,"
		           + "group_id INTEGER DEFAULT NULL,primary_directory TEXT DEFAULT NULL,"
		           + "secondary_directory TEXT DEFAULT NULL,document_id TEXT DEFAULT NULL,"
		           + "instance_id TEXT DEFAULT NULL,original_document_id TEXT DEFAULT NULL,"
		           + "relative_path TEXT DEFAULT NULL,volume_name TEXT DEFAULT NULL,"
		           + "artist_key TEXT DEFAULT NULL,album_key TEXT DEFAULT NULL,"
		           + "genre TEXT DEFAULT NULL,genre_key TEXT DEFAULT NULL,genre_id INTEGER,"
		           + "author TEXT DEFAULT NULL, bitrate INTEGER DEFAULT NULL,"
		           + "capture_framerate REAL DEFAULT NULL, cd_track_number TEXT DEFAULT NULL,"
		           + "compilation INTEGER DEFAULT NULL, disc_number TEXT DEFAULT NULL,"
		           + "is_favorite INTEGER DEFAULT 0, num_tracks INTEGER DEFAULT NULL,"
		           + "writer TEXT DEFAULT NULL, exposure_time TEXT DEFAULT NULL,"
		           + "f_number TEXT DEFAULT NULL, iso INTEGER DEFAULT NULL,"
		           + "scene_capture_type INTEGER DEFAULT NULL, generation_added INTEGER DEFAULT 0,"
		           + "generation_modified INTEGER DEFAULT 0, xmp BLOB DEFAULT NULL,"
		           + "_transcode_status INTEGER DEFAULT 0, _video_codec_type TEXT DEFAULT NULL,"
		           + "_modifier INTEGER DEFAULT 0, is_recording INTEGER DEFAULT 0,"
		           + "redacted_uri_id TEXT DEFAULT NULL, _user_id INTEGER DEFAULT 0,"
		           + " _special_format INTEGER DEFAULT NULL)");
		db.execSQL("CREATE TABLE audio_playlists_map (_id INTEGER PRIMARY KEY,"
		           + "audio_id INTEGER NOT NULL,playlist_id INTEGER NOT NULL,"
		           + "play_order INTEGER NOT NULL)");

		final String filterVolumeNames = "('" + VOLUME + "')";
		db.execSQL("CREATE VIEW audio_playlists AS SELECT * FROM files WHERE media_type=4");
		db.execSQL("CREATE VIEW audio AS SELECT * FROM files WHERE media_type=2");
		db.execSQL("CREATE VIEW video AS SELECT * FROM files WHERE media_type=3");
		db.execSQL("CREATE VIEW images AS SELECT * FROM files WHERE media_type=1");
		db.execSQL("CREATE VIEW downloads AS SELECT * FROM files WHERE is_download=1");
		db.execSQL("CREATE VIEW audio_artists AS SELECT "
		           + "  artist_id AS _id, MIN(artist) AS artist, artist_key AS artist_key"
		           + ", COUNT(DISTINCT album_id) AS number_of_albums, COUNT(DISTINCT _id) AS number_of_tracks"
		           + " FROM audio"
		           + " WHERE is_music=1 AND is_pending=0 AND is_trashed=0"
		           + " AND volume_name IN " + filterVolumeNames
		           + " GROUP BY artist_id");
		db.execSQL("CREATE VIEW audio_artists_albums AS SELECT "
		           + "  album_id AS _id, album_id AS album_id, MIN(album) AS album, album_key AS album_key"
		           + ", artist_id AS artist_id, artist AS artist, artist_key AS artist_key"
		           + ", (SELECT COUNT(*) FROM audio WHERE album_id = TEMP.album_id) AS numsongs"
		           + ", COUNT(DISTINCT _id) AS numsongs_by_artist, MIN(year) AS minyear, MAX(year) AS maxyear"
		           + ", NULL AS album_art"
		           + " FROM audio TEMP"
		           + " WHERE is_music=1 AND is_pending=0 AND is_trashed=0"
		           + " AND volume_name IN " + filterVolumeNames
		           + " GROUP BY album_id, artist_id");
		db.execSQL("CREATE VIEW audio_albums AS SELECT "
		           + "  album_id AS _id, album_id AS album_id, MIN(album) AS album, album_key AS album_key"
		           + ", artist_id AS artist_id, artist AS artist, artist_key AS artist_key"
		           + ", COUNT(DISTINCT _id) AS numsongs, COUNT(DISTINCT _id) AS numsongs_by_artist"
		           + ", MIN(year) AS minyear, MAX(year) AS maxyear, NULL AS album_art"
		           + " FROM audio"
		           + " WHERE is_music=1 AND is_pending=0 AND is_trashed=0"
		           + " AND volume_name IN " + filterVolumeNames
		           + " GROUP BY album_id");
		db.execSQL("CREATE VIEW audio_genres AS SELECT "
		           + "  genre_id AS _id, MIN(genre) AS name"
		           + " FROM audio"
		           + " WHERE is_pending=0 AND is_trashed=0 AND volume_name IN " + filterVolumeNames
		           + " GROUP BY genre_id");
	}

	/* the directories of the shared storage and their relative path: the user's folders, then the standard
	 * directories of the external storage that are not links to them */
	private static List<String[]> getRoots() {
		List<String[]> roots = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		File external = Environment.getExternalStorageDirectory();
		String[] userDirs = native_user_dirs();
		for (int i = 0; i < userDirs.length; i++) {
			if (userDirs[i] == null)
				continue;
			/* as the app sees it: through the link in the external storage (main.c's link_media_dirs()) if there
			 * is one, so the paths are those of the app's own file listing */
			File dir = new File(userDirs[i]);
			File link = new File(external, USER_DIR_NAMES[i]);
			try {
				if (link.getCanonicalPath().equals(dir.getCanonicalPath()))
					dir = link;
			} catch (IOException e) {
			}
			addRoot(roots, seen, dir, USER_DIR_NAMES[i] + "/");
		}
		for (String name : STANDARD_DIRECTORIES)
			addRoot(roots, seen, new File(external, name), name + "/");
		return roots;
	}

	private static void addRoot(List<String[]> roots, Set<String> seen, File dir, String relativePath) {
		try {
			if (dir.isDirectory() && seen.add(dir.getCanonicalPath()))
				roots.add(new String[] {dir.getAbsolutePath(), relativePath});
		} catch (IOException e) {
		}
	}

	/* ModernMediaScanner.scanDirectory(): new and changed files are (re)read, rows of files that are gone are
	 * deleted; hidden files and directories with .nomedia are left out */
	private static void scan(SQLiteDatabase db) {
		Set<String> present = new HashSet<>();
		db.beginTransaction();
		try {
			for (String[] root : getRoots())
				scanDirectory(db, new File(root[0]), root[1], present);
			List<Long> gone = new ArrayList<>();
			try (Cursor c = db.query("files", new String[] {"_id", "_data"}, null, null, null, null, null)) {
				while (c.moveToNext()) {
					if (!present.contains(c.getString(1)))
						gone.add(c.getLong(0));
				}
			}
			for (long id : gone)
				db.delete("files", "_id=" + id, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private static void scanDirectory(SQLiteDatabase db, File dir, String relativePath, Set<String> present) {
		File[] files = dir.listFiles();
		if (files == null || new File(dir, ".nomedia").exists())
			return;
		for (File file : files) {
			if (file.getName().startsWith("."))
				continue;
			if (file.isDirectory()) {
				scanDirectory(db, file, relativePath + file.getName() + "/", present);
				continue;
			}
			BasicFileAttributes attrs;
			try {
				attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			} catch (IOException e) {
				continue;
			}
			if (!attrs.isRegularFile())
				continue;
			String path = file.getAbsolutePath();
			present.add(path);
			long dateModified = attrs.lastModifiedTime().toMillis() / 1000;
			try (Cursor c = db.query("files", new String[] {"_id", "_size", "date_modified", "relative_path"}, "_data=?", new String[] {path}, null, null, null)) {
				if (c.moveToFirst()) {
					if (c.getLong(1) == attrs.size() && c.getLong(2) == dateModified && relativePath.equals(c.getString(3)))
						continue;
					db.update("files", scanFile(file, relativePath, attrs), "_id=" + c.getLong(0), null);
					continue;
				}
			}
			ContentValues values = scanFile(file, relativePath, attrs);
			/* MediaProvider sets it when the scanner first inserts the file; the time the file came is the
			 * nearest to that */
			values.put("date_added", attrs.creationTime().toMillis() / 1000);
			db.insert("files", null, values);
		}
	}

	/* the values of ModernMediaScanner.scanItemImage/Audio/Video/File() and FileUtils.computeValuesFromData() */
	private static ContentValues scanFile(File file, String relativePath, BasicFileAttributes attrs) {
		ContentValues values = new ContentValues();
		String mimeType = resolveMimeType(file.getName());
		int mediaType = resolveMediaType(mimeType);
		values.put("_data", file.getAbsolutePath());
		values.put("_size", attrs.size());
		values.put("date_modified", attrs.lastModifiedTime().toMillis() / 1000);
		values.put("mime_type", mimeType);
		values.put("media_type", mediaType);
		values.put("is_drm", 0);
		values.put("title", extractFileName(file.getName()));
		putPathValues(values, file, relativePath);
		try {
			switch (mediaType) {
				case 1:
					putImageValues(values, file, attrs);
					break;
				case 2:
					values.put("artist", MediaStore.UNKNOWN_STRING);
					values.put("album", file.getParentFile().getName());
					putAudioTypeValues(values, "/" + relativePath + file.getName());
					putRetrieverValues(values, file, false);
					computeAudioKeyValues(values);
					break;
				case 3:
					values.put("artist", MediaStore.UNKNOWN_STRING);
					values.put("album", file.getParentFile().getName());
					putRetrieverValues(values, file, true);
					break;
			}
		} catch (Exception e) {
			Log.w(TAG, "trouble scanning " + file + ": " + e);
		}
		return values;
	}

	/* FileUtils.computeValuesFromData() for a file at relativePath of the shared storage */
	private static void putPathValues(ContentValues values, File file, String relativePath) {
		values.put("volume_name", VOLUME);
		values.put("relative_path", relativePath);
		values.put("_display_name", file.getName());
		values.put("is_pending", 0);
		values.put("is_trashed", 0);
		values.put("bucket_id", file.getAbsoluteFile().getParent().toLowerCase(Locale.ROOT).hashCode());
		values.put("bucket_display_name", file.getParentFile().getName());
		/* MediaProvider: isDownload() */
		values.put("is_download", relativePath.startsWith("Download/") ? 1 : 0);
	}

	private static void putImageValues(ContentValues values, File file, BasicFileAttributes attrs) throws IOException {
		ExifInterface exif = new ExifInterface(file.getAbsolutePath());
		int width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
		int height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
		if (width <= 0 || height <= 0) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(file.getAbsolutePath(), options);
			width = options.outWidth;
			height = options.outHeight;
		}
		if (width > 0 && height > 0) {
			values.put("width", width);
			values.put("height", height);
			values.put("resolution", width + "×" + height);
		}
		Long dateTaken = parseDateTaken(exif, attrs.lastModifiedTime().toMillis());
		if (dateTaken != null)
			values.put("datetaken", dateTaken);
		switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
			case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
			case ExifInterface.ORIENTATION_NORMAL:
				values.put("orientation", 0);
				break;
			case ExifInterface.ORIENTATION_TRANSPOSE:
			case ExifInterface.ORIENTATION_ROTATE_90:
				values.put("orientation", 90);
				break;
			case ExifInterface.ORIENTATION_FLIP_VERTICAL:
			case ExifInterface.ORIENTATION_ROTATE_180:
				values.put("orientation", 180);
				break;
			case ExifInterface.ORIENTATION_TRANSVERSE:
			case ExifInterface.ORIENTATION_ROTATE_270:
				values.put("orientation", 270);
				break;
		}
		putOptional(values, "description", exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION));
	}

	/* ModernMediaScanner.parseOptionalDateTaken() */
	private static Long parseDateTaken(ExifInterface exif, long lastModifiedTime) {
		final long originalTime = exif.getDateTimeOriginal();
		if (originalTime <= 0)
			return null;
		if (exif.hasAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL))
			return originalTime;
		final long smallestZone = 15 * 60 * 1000;
		final long gpsTime = exif.getGpsDateTime();
		for (long time : new long[] {gpsTime, lastModifiedTime}) {
			if (time > 0) {
				final long offset = time - originalTime;
				if (Math.abs(offset) < 24 * 60 * 60 * 1000) {
					final long rounded = Math.round((float)offset / smallestZone) * smallestZone;
					return originalTime + rounded;
				}
			}
		}
		return null;
	}

	private static final Pattern PATTERN_YEAR = Pattern.compile("([1-9][0-9][0-9][0-9])");

	/* ModernMediaScanner.withRetrieverValues(), parseOptionalTrack() and the video values */
	private static void putRetrieverValues(ContentValues values, File file, boolean video) throws IOException {
		try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
			mmr.setDataSource(file.getAbsolutePath());
			String date = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
			if (date != null) {
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US);
				format.setTimeZone(TimeZone.getTimeZone("UTC"));
				try {
					long time = format.parse(date).getTime();
					if (time > 0)
						values.put("datetaken", time);
				} catch (ParseException e) {
				}
			}
			putOptional(values, "cd_track_number", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER));
			putOptional(values, "album", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
			String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
			putOptional(values, "artist", isPresent(artist) ? artist : mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST));
			putOptional(values, "author", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR));
			putOptional(values, "composer", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER));
			putOptional(values, "genre", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
			putOptional(values, "title", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
			String year = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
			Matcher m = year != null ? PATTERN_YEAR.matcher(year) : null;
			if (m != null && m.find())
				values.put("year", Integer.parseInt(m.group(1)));
			putOptional(values, "duration", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
			putOptional(values, "num_tracks", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS));
			putOptional(values, "writer", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER));
			putOptional(values, "album_artist", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST));
			putOptional(values, "disc_number", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER));
			putOptional(values, "compilation", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION));
			putOptional(values, "bitrate", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
			if (video) {
				String width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
				String height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
				putOptional(values, "width", width);
				putOptional(values, "height", height);
				if (isPresent(width) && isPresent(height))
					values.put("resolution", width + "×" + height);
			} else {
				Integer disc = parseNumerator(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER));
				Integer track = parseNumerator(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER));
				if (track != null)
					values.put("track", disc != null ? disc * 1000 + track : track);
			}
		}
	}

	/* ModernMediaScanner.parseOptional() */
	private static boolean isPresent(String value) {
		return value != null && value.trim().length() > 0 && !value.equals("-1");
	}

	private static void putOptional(ContentValues values, String column, String value) {
		if (isPresent(value))
			values.put(column, value);
	}

	/* ModernMediaScanner.parseOptionalNumerator() */
	private static Integer parseNumerator(String value) {
		if (!isPresent(value))
			return null;
		int fractionIndex = value.indexOf('/');
		try {
			return Integer.parseInt(fractionIndex != -1 ? value.substring(0, fractionIndex) : value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/* FileUtils.computeAudioTypeValuesFromData() on the path in the shared storage */
	private static void putAudioTypeValues(ContentValues values, String path) {
		String lowPath = path.toLowerCase(Locale.ROOT);
		boolean anyMatch = false;
		for (String[] type : AUDIO_TYPES) {
			boolean match = lowPath.contains('/' + type[0].toLowerCase(Locale.ROOT) + '/');
			values.put(type[1], match ? 1 : 0);
			anyMatch |= match;
		}
		if (!anyMatch)
			values.put("is_music", 1);
	}

	/* MediaProvider.computeAudioKeyValues(); the IDs hash the key with FNV-1a instead of FarmHash */
	private static void computeAudioKeyValues(ContentValues values) {
		computeAudioKeyValue(values, "title", "title_key", null, 0);
		computeAudioKeyValue(values, "artist", "artist_key", "artist_id", 0);
		computeAudioKeyValue(values, "genre", "genre_key", "genre_id", 0);
		int hashCode = 0;
		String albumArtist = values.getAsString("album_artist");
		if (!TextUtils.isEmpty(albumArtist)) {
			hashCode = albumArtist.hashCode();
		} else {
			String path = values.getAsString("_data");
			if (!TextUtils.isEmpty(path))
				hashCode = path.substring(0, path.lastIndexOf('/')).hashCode();
		}
		computeAudioKeyValue(values, "album", "album_key", "album_id", hashCode);
	}

	private static void computeAudioKeyValue(ContentValues values, String focus, String focusKey, String focusId, int hashValue) {
		values.remove(focusKey);
		if (focusId != null)
			values.remove(focusId);
		String value = values.getAsString(focus);
		if (TextUtils.isEmpty(value))
			return;
		String key = keyFor(value);
		values.put(focusKey, key);
		if (focusId != null) {
			long hash = 0xcbf29ce484222325L;
			for (byte b : (key + hashValue).getBytes(StandardCharsets.UTF_8)) {
				hash ^= b & 0xff;
				hash *= 0x100000001b3L;
			}
			// Many apps break if we generate negative IDs, so trim off the
			// highest bit to ensure we're always unsigned
			values.put(focusId, hash & ~(1L << 63));
		}
	}

	private static final Pattern PATTERN_TRIM_BEFORE = Pattern.compile("(?i)(^(the|an|a) |,\\s*(the|an|a)$|[^\\w\\s]|^\\s+|\\s+$)");
	private static final Pattern PATTERN_TRIM_AFTER = Pattern.compile("(^(00)+|(00)+$)");

	/* MediaStore.Audio.keyFor() */
	private static String keyFor(String name) {
		if (TextUtils.isEmpty(name))
			return "";
		if (MediaStore.UNKNOWN_STRING.equals(name))
			return "01";
		final boolean sortFirst = name.startsWith("\001");
		name = PATTERN_TRIM_BEFORE.matcher(name).replaceAll("");
		if (TextUtils.isEmpty(name))
			return "";
		final Collator c = Collator.getInstance(Locale.ROOT);
		c.setStrength(Collator.PRIMARY);
		final StringBuilder sb = new StringBuilder();
		for (byte b : c.getCollationKey(name).toByteArray())
			sb.append(String.format("%02x", b));
		name = PATTERN_TRIM_AFTER.matcher(sb.toString()).replaceAll("");
		if (sortFirst)
			name = "01" + name;
		return name;
	}

	/* FileUtils.extractFileName(): the display name without extension */
	private static String extractFileName(String name) {
		int lastDot = name.lastIndexOf('.');
		return lastDot != -1 ? name.substring(0, lastDot) : name;
	}

	/* MimeUtils.resolveMimeType() */
	private static String resolveMimeType(String name) {
		int lastDot = name.lastIndexOf('.');
		String mimeType = lastDot != -1 ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(lastDot + 1).toLowerCase(Locale.ROOT)) : null;
		return mimeType != null ? mimeType : "application/octet-stream";
	}

	/* MimeUtils.resolveMediaType() */
	private static int resolveMediaType(String mimeType) {
		String type = mimeType.toLowerCase(Locale.ROOT);
		switch (type) {
			case "application/vnd.apple.mpegurl":
			case "application/vnd.ms-wpl":
			case "application/x-extension-smpl":
			case "application/x-mpegurl":
			case "application/xspf+xml":
			case "audio/mpegurl":
			case "audio/x-mpegurl":
			case "audio/x-scpls":
				return 4;
			case "application/lrc":
			case "application/smil+xml":
			case "application/ttml+xml":
			case "application/x-extension-cap":
			case "application/x-extension-srt":
			case "application/x-extension-sub":
			case "application/x-extension-vtt":
			case "application/x-subrip":
			case "text/vtt":
				return 5;
		}
		if (type.startsWith("audio/"))
			return 2;
		if (type.startsWith("video/"))
			return 3;
		if (type.startsWith("image/"))
			return 1;
		/* MimeUtils.isDocumentMimeType(): text, PDF, EPUB and the office formats */
		if (type.startsWith("text/") || type.equals("application/pdf") || type.equals("application/epub+zip")
		    || type.equals("application/msword") || type.equals("application/rtf") || type.startsWith("application/vnd.ms-")
		    || type.startsWith("application/vnd.oasis.opendocument.") || type.startsWith("application/vnd.openxmlformats-officedocument."))
			return 6;
		return 0;
	}

	/* the query of MediaProvider.queryInternal() and getQueryBuilder(): the table or view of the URI with its
	 * standard filters, then the app's selection, grouping, sorting and limit straight on SQLite */
	@Override
	public Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal signal) {
		queryArgs = queryArgs != null ? queryArgs : Bundle.EMPTY;
		final int match = MATCHER.match(uri);
		switch (match) {
			case MEDIA_SCANNER: {
				MatrixCursor c = new MatrixCursor(new String[] {"volume"});
				c.addRow(new String[] {null});
				return c;
			}
			case FS_ID: {
				MatrixCursor c = new MatrixCursor(new String[] {"fsid"});
				c.addRow(new Integer[] {-1});
				return c;
			}
			case VERSION: {
				MatrixCursor c = new MatrixCursor(new String[] {"version"});
				c.addRow(new Integer[] {DATABASE_VERSION});
				return c;
			}
		}
		SQLiteQueryBuilder qb = getQueryBuilder(match, uri, queryArgs);
		String sortOrder = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER);
		if (sortOrder == null && queryArgs.containsKey(ContentResolver.QUERY_ARG_SORT_COLUMNS))
			sortOrder = ContentResolver.createSqlSortClause(queryArgs);
		String groupBy = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_GROUP_BY);
		String[] groupColumns = queryArgs.getStringArray(ContentResolver.QUERY_ARG_GROUP_COLUMNS);
		if (groupColumns != null && groupColumns.length > 0)
			groupBy = TextUtils.join(", ", groupColumns);
		/* DatabaseUtils.resolveLimit() and recoverAbusiveLimit(); "offset, limit" is SQLite's form of LIMIT n OFFSET m
		 * that SQLiteQueryBuilder accepts */
		String limit = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_LIMIT);
		if (queryArgs.containsKey(ContentResolver.QUERY_ARG_LIMIT)) {
			limit = Integer.toString(queryArgs.getInt(ContentResolver.QUERY_ARG_LIMIT));
			if (queryArgs.containsKey(ContentResolver.QUERY_ARG_OFFSET))
				limit = queryArgs.getInt(ContentResolver.QUERY_ARG_OFFSET) + ", " + limit;
		} else if (limit == null) {
			limit = uri.getQueryParameter(MediaStore.PARAM_LIMIT);
		}
		return qb.query(getDatabase(), projection, queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SELECTION),
		                queryArgs.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS), groupBy,
		                queryArgs.getString(ContentResolver.QUERY_ARG_SQL_HAVING), sortOrder, limit, signal);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Bundle queryArgs = new Bundle();
		if (selection != null)
			queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection);
		if (selectionArgs != null)
			queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs);
		if (sortOrder != null)
			queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder);
		return query(uri, projection, queryArgs, null);
	}

	/* MediaProvider.getQueryBuilder() for a query; for writing, the rows of this app only */
	private static SQLiteQueryBuilder getQueryBuilder(int match, Uri uri, Bundle queryArgs) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		if (uri.getBooleanQueryParameter("distinct", false))
			qb.setDistinct(true);
		int matchPending = queryArgs.getInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_DEFAULT);
		int matchTrashed = queryArgs.getInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_DEFAULT);
		int matchFavorite = queryArgs.getInt(MediaStore.QUERY_ARG_MATCH_FAVORITE, MediaStore.MATCH_DEFAULT);
		if (MediaStore.getIncludePending(uri))
			matchPending = MediaStore.MATCH_INCLUDE;
		if (matchPending == MediaStore.MATCH_DEFAULT)
			matchPending = MediaStore.MATCH_EXCLUDE;
		if (matchTrashed == MediaStore.MATCH_DEFAULT)
			matchTrashed = MediaStore.MATCH_EXCLUDE;
		if (matchFavorite == MediaStore.MATCH_DEFAULT)
			matchFavorite = MediaStore.MATCH_INCLUDE;
		List<String> segments = uri.getPathSegments();
		String volumeName = segments.isEmpty() ? VOLUME : segments.get(0);
		String includeVolumes = DatabaseUtils.sqlEscapeString(MediaStore.VOLUME_EXTERNAL.equals(volumeName) ? VOLUME : volumeName);
		boolean media = true;
		/* the parts of the WHERE clause, as appendWhereStandalone() puts them */
		List<String> wheres = new ArrayList<>();
		switch (match) {
			case IMAGES_MEDIA_ID:
			case AUDIO_MEDIA_ID:
			case VIDEO_MEDIA_ID:
			case DOWNLOADS_ID:
			case FILES_ID:
				wheres.add("_id=" + Long.parseLong(segments.get(match == FILES_ID || match == DOWNLOADS_ID ? 2 : 3)));
				matchPending = MediaStore.MATCH_INCLUDE;
				matchTrashed = MediaStore.MATCH_INCLUDE;
				break;
		}
		switch (match) {
			case IMAGES_MEDIA_ID:
			case IMAGES_MEDIA:
				qb.setTables("images");
				break;
			case AUDIO_MEDIA_ID:
			case AUDIO_MEDIA:
				qb.setTables("audio");
				break;
			case VIDEO_MEDIA_ID:
			case VIDEO_MEDIA:
				qb.setTables("video");
				break;
			case DOWNLOADS_ID:
			case DOWNLOADS:
				qb.setTables("downloads");
				break;
			case FILES_ID:
			case FILES:
				qb.setTables("files");
				break;
			case AUDIO_GENRES_ID_MEMBERS:
				wheres.add("genre_id=" + Long.parseLong(segments.get(3)));
				// fall-through
			case AUDIO_GENRES_ALL_MEMBERS:
				qb.setTables("audio");
				break;
			default:
				media = false;
				switch (match) {
					case IMAGES_THUMBNAILS_ID:
						wheres.add("_id=" + Long.parseLong(segments.get(3)));
						// fall-through
					case IMAGES_THUMBNAILS:
						qb.setTables("thumbnails");
						break;
					case VIDEO_THUMBNAILS_ID:
						wheres.add("_id=" + Long.parseLong(segments.get(3)));
						// fall-through
					case VIDEO_THUMBNAILS:
						qb.setTables("videothumbnails");
						break;
					case AUDIO_MEDIA_ID_GENRES_ID:
						wheres.add("_id=" + Long.parseLong(segments.get(5)));
						// fall-through
					case AUDIO_MEDIA_ID_GENRES:
						wheres.add("_id IN (SELECT genre_id FROM audio WHERE _id=" + Long.parseLong(segments.get(3)) + ")");
						qb.setTables("audio_genres");
						break;
					case AUDIO_GENRES_ID:
						wheres.add("_id=" + Long.parseLong(segments.get(3)));
						// fall-through
					case AUDIO_GENRES:
						qb.setTables("audio_genres");
						break;
					case AUDIO_PLAYLISTS_ID:
						wheres.add("_id=" + Long.parseLong(segments.get(3)));
						// fall-through
					case AUDIO_PLAYLISTS:
						qb.setTables("audio_playlists");
						wheres.add("volume_name IN (" + includeVolumes + ")");
						break;
					case AUDIO_PLAYLISTS_ID_MEMBERS_ID:
						wheres.add("audio_playlists_map._id=" + Long.parseLong(segments.get(5)));
						// fall-through
					case AUDIO_PLAYLISTS_ID_MEMBERS:
						wheres.add("playlist_id=" + Long.parseLong(segments.get(3)) + " AND audio._id=audio_id");
						qb.setTables("audio_playlists_map, audio");
						break;
					case AUDIO_ARTISTS_ID:
						wheres.add("_id=" + Long.parseLong(segments.get(3)));
						// fall-through
					case AUDIO_ARTISTS:
						qb.setTables("audio_artists");
						break;
					case AUDIO_ARTISTS_ID_ALBUMS:
						wheres.add("artist_id=" + Long.parseLong(segments.get(3)));
						qb.setTables("audio_artists_albums");
						break;
					case AUDIO_ALBUMART_ID:
						wheres.add("album_id=" + Long.parseLong(segments.get(3)));
						// fall-through
					case AUDIO_ALBUMART:
						qb.setTables("album_art");
						break;
					case AUDIO_ALBUMS_ID:
						wheres.add("_id=" + Long.parseLong(segments.get(3)));
						// fall-through
					case AUDIO_ALBUMS:
						qb.setTables("audio_albums");
						break;
					default:
						throw new UnsupportedOperationException("Unknown or unsupported URL: " + uri.toString());
				}
		}
		if (media) {
			appendMatch(wheres, "is_pending", matchPending);
			appendMatch(wheres, "is_trashed", matchTrashed);
			appendMatch(wheres, "is_favorite", matchFavorite);
			wheres.add("volume_name IN (" + includeVolumes + ")");
		}
		if (!wheres.isEmpty())
			qb.appendWhere("(" + TextUtils.join(") AND (", wheres) + ")");
		return qb;
	}

	/* MediaProvider.appendWhereStandaloneMatch() */
	private static void appendMatch(List<String> wheres, String column, int match) {
		switch (match) {
			case MediaStore.MATCH_EXCLUDE:
				wheres.add(column + "=0");
				break;
			case MediaStore.MATCH_ONLY:
				wheres.add(column + "=1");
				break;
		}
	}

	/* the row of an item URI, or null */
	private Cursor queryItem(Uri uri, String[] projection) {
		Bundle args = new Bundle();
		args.putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE);
		args.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
		Cursor c = query(uri, projection, args, null);
		if (c.moveToFirst())
			return c;
		c.close();
		return null;
	}

	@Override
	public String getType(Uri uri) {
		final int match = MATCHER.match(uri);
		switch (match) {
			case IMAGES_MEDIA_ID:
			case AUDIO_MEDIA_ID:
			case VIDEO_MEDIA_ID:
			case DOWNLOADS_ID:
			case FILES_ID:
				try (Cursor c = queryItem(uri, new String[] {"mime_type"})) {
					return c != null ? c.getString(0) : null;
				}
			case AUDIO_ALBUMART_ID:
			case AUDIO_ALBUMART_FILE_ID:
				return "image/jpeg";
			case IMAGES_MEDIA:
				return MediaStore.Images.Media.CONTENT_TYPE;
			case AUDIO_MEDIA:
				return MediaStore.Audio.Media.CONTENT_TYPE;
			case VIDEO_MEDIA:
				return MediaStore.Video.Media.CONTENT_TYPE;
			case DOWNLOADS:
				return MediaStore.Downloads.CONTENT_TYPE;
			case AUDIO_ARTISTS:
				return MediaStore.Audio.Artists.CONTENT_TYPE;
			case AUDIO_ARTISTS_ID:
				return MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE;
			case AUDIO_ALBUMS:
				return MediaStore.Audio.Albums.CONTENT_TYPE;
			case AUDIO_ALBUMS_ID:
				return MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE;
			case AUDIO_GENRES:
				return MediaStore.Audio.Genres.CONTENT_TYPE;
			case AUDIO_GENRES_ID:
				return MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE;
			case AUDIO_PLAYLISTS:
				return MediaStore.Audio.Playlists.CONTENT_TYPE;
			case AUDIO_PLAYLISTS_ID:
				return MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE;
		}
		return null;
	}

	/* the file of an item; writing only to the items this app inserted, which are read again when the file is
	 * closed (MediaProvider's onOpenFileClose() scans it); album art as the picture embedded in a song */
	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		final int match = MATCHER.match(uri);
		if (match == AUDIO_ALBUMART_ID || match == AUDIO_ALBUMART_FILE_ID)
			return openAlbumArt(uri, match);
		if (match != IMAGES_MEDIA_ID && match != AUDIO_MEDIA_ID && match != VIDEO_MEDIA_ID && match != DOWNLOADS_ID && match != FILES_ID)
			throw new FileNotFoundException("No file for " + uri);
		String path;
		String owner;
		long id;
		try (Cursor c = queryItem(uri, new String[] {"_data", "owner_package_name", "_id"})) {
			if (c == null)
				throw new FileNotFoundException("No item at " + uri);
			path = c.getString(0);
			owner = c.getString(1);
			id = c.getLong(2);
		}
		int modeBits = ParcelFileDescriptor.parseMode(mode);
		if ((modeBits & ParcelFileDescriptor.MODE_READ_ONLY) == modeBits)
			return ParcelFileDescriptor.open(new File(path), modeBits);
		if (!packageName().equals(owner))
			throw new SecurityException(packageName() + " has no access to " + uri);
		final File file = new File(path);
		try {
			return ParcelFileDescriptor.open(file, modeBits, getHandler(), e -> {
				ContentValues values = new ContentValues();
				values.put("_size", file.length());
				values.put("date_modified", file.lastModified() / 1000);
				getDatabase().update("files", values, "_id=" + id, null);
			});
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw new FileNotFoundException(e.toString());
		}
	}

	private static Handler handler;

	private static synchronized Handler getHandler() {
		if (handler == null) {
			HandlerThread thread = new HandlerThread(TAG);
			thread.start();
			handler = new Handler(thread.getLooper());
		}
		return handler;
	}

	private ParcelFileDescriptor openAlbumArt(Uri uri, int match) throws FileNotFoundException {
		long id = Long.parseLong(uri.getPathSegments().get(3));
		String where = match == AUDIO_ALBUMART_ID ? "album_id=" + id : "_id=" + id;
		List<String> paths = new ArrayList<>();
		try (Cursor c = getDatabase().query("audio", new String[] {"_data"}, where + " AND is_pending=0 AND is_trashed=0", null, null, null, "_id")) {
			while (c.moveToNext())
				paths.add(c.getString(0));
		}
		for (String path : paths) {
			byte[] picture;
			try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
				mmr.setDataSource(path);
				picture = mmr.getEmbeddedPicture();
			} catch (Exception e) {
				continue;
			}
			if (picture == null)
				continue;
			try {
				File file = File.createTempFile("albumart", null, new File(ATLLoadedApp.getPrimaryApplication().getApplication().getDataDir(), "atl_media"));
				try (FileOutputStream out = new FileOutputStream(file)) {
					out.write(picture);
				}
				ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
				file.delete();
				return fd;
			} catch (IOException e) {
				throw new FileNotFoundException(e.toString());
			}
		}
		throw new FileNotFoundException("No album art found for " + uri);
	}

	/* MediaProvider.insertFile() for a new item: the file is created in the standard directory of ATL's external
	 * storage, Download/ is the user's Download folder where main.c's link_media_dirs() linked it; the other
	 * links to the user's folders (Music, Movies) are not written to (null, as a provider that fails) */
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		final int match = MATCHER.match(uri);
		String defaultDir;
		switch (match) {
			case IMAGES_MEDIA:
				defaultDir = "Pictures";
				break;
			case AUDIO_MEDIA:
				defaultDir = "Music";
				break;
			case VIDEO_MEDIA:
				defaultDir = "Movies";
				break;
			case DOWNLOADS:
				defaultDir = "Download";
				break;
			case FILES:
				defaultDir = null;
				break;
			default:
				throw new UnsupportedOperationException("Invalid URI " + uri);
		}
		ContentValues values = new ContentValues(initialValues);
		for (String column : new String[] {"_id", "_data", "volume_name", "owner_package_name", "media_type", "bucket_id",
		                                   "bucket_display_name", "is_download", "date_added", "date_modified", "_size"})
			values.remove(column);
		String displayName = values.getAsString("_display_name");
		String mimeType = values.getAsString("mime_type");
		if (TextUtils.isEmpty(displayName))
			displayName = String.valueOf(System.currentTimeMillis());
		/* MediaProvider.ensureFileColumns(): the extension of the MIME type, else the type of the extension */
		if (mimeType == null) {
			mimeType = resolveMimeType(displayName);
		} else if (displayName.indexOf('.') == -1 || !mimeType.equalsIgnoreCase(resolveMimeType(displayName))) {
			String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
			if (extension != null)
				displayName = displayName + "." + extension;
		}
		int mediaType = resolveMediaType(mimeType);
		if (defaultDir == null)
			defaultDir = mediaType == 1 ? "Pictures" : mediaType == 2 ? "Music"
			                                     : mediaType == 3     ? "Movies"
			                                                          : "Download";
		String relativePath = values.getAsString("relative_path");
		if (TextUtils.isEmpty(relativePath))
			relativePath = defaultDir + "/";
		if (!relativePath.endsWith("/"))
			relativePath += "/";

		File external = Environment.getExternalStorageDirectory();
		File dir = new File(external, relativePath);
		File file;
		try {
			String base = external.getCanonicalPath() + "/";
			String download = new File(external, "Download").getCanonicalPath() + "/";
			String target = dir.getCanonicalPath() + "/";
			if (!(target.startsWith(base) || relativePath.startsWith("Download/") && target.startsWith(download)) || relativePath.contains("../")) {
				Log.w(TAG, "insert " + uri + ": " + relativePath + " is not in the app's storage, not written");
				return null;
			}
			dir.mkdirs();
			file = new File(dir, displayName);
			/* FileUtils.buildUniqueFile() */
			String name = extractFileName(displayName);
			String extension = displayName.substring(name.length());
			for (int n = 1; !file.createNewFile(); n++)
				file = new File(dir, name + " (" + n + ")" + extension);
		} catch (IOException e) {
			Log.w(TAG, "insert " + uri + ": " + e);
			return null;
		}

		long now = System.currentTimeMillis() / 1000;
		values.put("_data", file.getAbsolutePath());
		values.put("_size", 0);
		values.put("date_added", now);
		values.put("date_modified", now);
		values.put("mime_type", mimeType);
		values.put("media_type", mediaType);
		values.put("owner_package_name", packageName());
		if (!values.containsKey("title"))
			values.put("title", extractFileName(file.getName()));
		Integer pending = values.getAsInteger("is_pending");
		putPathValues(values, file, relativePath);
		values.put("is_pending", pending != null ? pending : 0);
		if (mediaType == 2) {
			putAudioTypeValues(values, "/" + relativePath + file.getName());
			computeAudioKeyValues(values);
		}
		long id = getDatabase().insertOrThrow("files", null, values);
		String volumeName = uri.getPathSegments().get(0);
		switch (match) {
			case FILES:
				return MediaStore.Files.getContentUri(volumeName, id);
			default:
				return ContentUris.withAppendedId(uri, id);
		}
	}

	/* the rows of this app that the URI and selection name (MediaProvider's write access: other items are not
	 * matched) */
	private Cursor queryOwn(Uri uri, String selection, String[] selectionArgs) {
		Bundle args = new Bundle();
		args.putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE);
		args.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
		String own = "owner_package_name=" + DatabaseUtils.sqlEscapeString(packageName());
		args.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection != null ? "(" + selection + ") AND " + own : own);
		args.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs);
		return query(uri, new String[] {"_id", "_data"}, args, null);
	}

	/* the app's own items; the file is read again for its size and time (as the scan after IS_PENDING=0),
	 * moving it by _display_name or relative_path is not supported */
	@Override
	public int update(Uri uri, ContentValues initialValues, String selection, String[] selectionArgs) {
		ContentValues values = new ContentValues(initialValues);
		for (String column : new String[] {"_id", "_data", "volume_name", "owner_package_name", "media_type", "_display_name",
		                                   "relative_path", "bucket_id", "bucket_display_name", "is_download", "_size"})
			values.remove(column);
		int count = 0;
		try (Cursor c = queryOwn(uri, selection, selectionArgs)) {
			while (c.moveToNext()) {
				File file = new File(c.getString(1));
				values.put("_size", file.length());
				values.put("date_modified", file.lastModified() / 1000);
				count += getDatabase().update("files", values, "_id=" + c.getLong(0), null);
			}
		}
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		try (Cursor c = queryOwn(uri, selection, selectionArgs)) {
			while (c.moveToNext()) {
				new File(c.getString(1)).delete();
				count += getDatabase().delete("files", "_id=" + c.getLong(0), null);
			}
		}
		return count;
	}

	private static native String[] native_user_dirs();
}
