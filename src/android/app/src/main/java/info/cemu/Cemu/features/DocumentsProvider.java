// Based on:
// Skyline
// SPDX-License-Identifier: MPL-2.0
// Copyright © 2022 Skyline Team and Contributors (https://github.com/skyline-emu/)
package info.cemu.Cemu.features;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import info.cemu.Cemu.CemuApplication;
import info.cemu.Cemu.BuildConfig;
import info.cemu.Cemu.R;

public class DocumentsProvider extends android.provider.DocumentsProvider {
    private final File baseDirectory;
    private final String applicationName = CemuApplication.getApplication().getApplicationInfo().loadLabel(CemuApplication.getApplication().getPackageManager()).toString();

    public DocumentsProvider() {
        try {
            baseDirectory = CemuApplication.getApplication().getInternalFolder().getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final String[] DEFAULT_ROOT_PROJECTION = {
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
    };

    private final String[] DEFAULT_DOCUMENT_PROJECTION = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
    };

    public static final String ROOT_ID = "root";
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        var cursor = new MatrixCursor(projection == null ? DEFAULT_ROOT_PROJECTION : projection);
        cursor.newRow().add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID)
                .add(DocumentsContract.Root.COLUMN_SUMMARY, null)
                .add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_SUPPORTS_CREATE | DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD)
                .add(DocumentsContract.Root.COLUMN_TITLE, applicationName)
                .add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocumentId(baseDirectory))
                .add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
                .add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, baseDirectory.getFreeSpace())
                .add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher);
        return cursor;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        var cursor = new MatrixCursor(projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        includeFile(cursor, documentId, null);
        return cursor;
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        if (parentDocumentId == null || documentId == null) {
            return false;
        }
        return documentId.startsWith(parentDocumentId);
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
        var parentFile = getFile(parentDocumentId);
        var newFile = resolveWithoutConflict(parentFile, displayName);

        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
            if (!newFile.mkdir()) {
                throw new FileNotFoundException("Failed to create directory");
            }
        } else {
            try {
                if (!newFile.createNewFile()) {
                    throw new FileNotFoundException("Failed to create file");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return getDocumentId(newFile);
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        var file = getFile(documentId);
        if (file.isDirectory()) {
            deleteFolder(file);
            return;
        }
        if (!file.delete()) {
            throw new FileNotFoundException("Couldn't delete document with ID " + documentId);
        }
    }

    private void deleteFolder(File dirFile) throws FileNotFoundException {
        if (!dirFile.isDirectory()) {
            return;
        }
        var files = dirFile.listFiles();
        if (files == null) {
            return;
        }
        for (var file : files) {
            if (file.isDirectory()) {
                deleteFolder(file);
                continue;
            }
            if (!file.delete()) {
                throw new FileNotFoundException("Couldn't delete file " + file.getPath());
            }
        }
        if (!dirFile.delete()) {
            throw new FileNotFoundException("Couldn't delete file " + dirFile.getPath());
        }
    }

    @Override
    public void removeDocument(String documentId, String parentDocumentId) throws FileNotFoundException {
        var parent = getFile(parentDocumentId);
        var file = getFile(documentId);

        if (!(parent.equals(file) || file.getParentFile() == null || file.getParentFile().equals(parent))) {
            throw new FileNotFoundException("Couldn't delete document with ID " + documentId);
        }
        if (file.isDirectory()) {
            deleteFolder(file);
            return;
        }
        if (!file.delete()) {
            throw new FileNotFoundException("Couldn't delete document with ID " + documentId);
        }
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        if (displayName == null) {
            throw new FileNotFoundException("Couldn't rename document " + documentId + " as the new name is null");
        }

        var sourceFile = getFile(documentId);
        var sourceParentFile = sourceFile.getParentFile();
        if (sourceParentFile == null) {
            throw new FileNotFoundException("Couldn't rename document '" + documentId + "' as it has no parent");
        }
        var destFile = resolve(sourceParentFile, displayName);

        try {
            if (!sourceFile.renameTo(destFile)) {
                throw new FileNotFoundException("Couldn't rename document from '" + sourceFile.getName() + "' to '" + destFile.getName() + "'");
            }
        } catch (Exception exception) {
            throw new FileNotFoundException("Couldn't rename document from '" + sourceFile.getName() + "' to '" + destFile.getName() + "':" + exception.getMessage());
        }

        return getDocumentId(destFile);
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        var parent = getFile(targetParentDocumentId);
        var oldFile = getFile(sourceDocumentId);
        var newFile = resolveWithoutConflict(parent, oldFile.getName());

        try {
            if (!(newFile.createNewFile() && newFile.setWritable(true) && newFile.setReadable(true))) {
                throw new IOException("Couldn't create new file");
            }
            try (var inputStream = new FileInputStream(oldFile); var outputStream = new FileOutputStream(newFile)) {
                byte[] b = new byte[1024];
                int len;
                while ((len = inputStream.read(b, 0, 1024)) > 0) {
                    outputStream.write(b, 0, len);
                }
            }
        } catch (IOException exception) {
            throw new FileNotFoundException("Couldn't copy document '" + sourceDocumentId + "': " + exception.getMessage());
        }
        return getDocumentId(newFile);
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        try {
            var newDocumentId = copyDocument(sourceDocumentId, sourceParentDocumentId, targetParentDocumentId);
            removeDocument(sourceDocumentId, sourceParentDocumentId);
            return newDocumentId;
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Couldn't move document '" + sourceDocumentId + "'");
        }
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        var cursor = new MatrixCursor(projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        var parent = getFile(parentDocumentId);
        var files = parent.listFiles();
        if (files == null) {
            return cursor;
        }
        for (var file : files) {
            includeFile(cursor, null, file);
        }
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        var file = getFile(documentId);
        var accessMode = ParcelFileDescriptor.parseMode(mode);
        return ParcelFileDescriptor.open(file, accessMode);
    }

    private File resolve(File file, String other) {
        return file.toPath().resolve(other).toFile();
    }

    private String copyDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        if (!isChildDocument(sourceParentDocumentId, sourceDocumentId)) {
            throw new FileNotFoundException("Couldn't copy document '" + sourceDocumentId + "' as its parent is not '" + sourceParentDocumentId + "'");
        }
        return copyDocument(sourceDocumentId, targetParentDocumentId);
    }

    private File resolveWithoutConflict(File originalFile, String name) {
        var file = resolve(originalFile, name);
        if (!file.exists()) {
            return file;
        }

        // Makes sure two files don't have the same name by adding a number to the end
        var noConflictId = 1;
        var periodIndex = name.lastIndexOf('.');
        var extension = "";
        var baseName = name;
        if (periodIndex != -1) {
            baseName = name.substring(0, periodIndex);
            extension = name.substring(periodIndex);
        }
        while (file.exists()) {
            String newFileName = baseName + " (" + noConflictId + ")" + extension;
            file = file.toPath().resolve(newFileName).toFile();
        }
        return file;
    }

    private void includeFile(MatrixCursor cursor, String documentId, File file) throws FileNotFoundException {
        var localDocumentId = documentId == null ? getDocumentId(file) : documentId;
        var localFile = file == null ? getFile(documentId) : file;
        int flags = 0;
        if (localFile.isDirectory() && localFile.canWrite()) {
            flags = DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
        } else if (localFile.canWrite()) {
            flags = DocumentsContract.Document.FLAG_SUPPORTS_WRITE
                    | DocumentsContract.Document.FLAG_SUPPORTS_MOVE
                    | DocumentsContract.Document.FLAG_SUPPORTS_COPY
                    | DocumentsContract.Document.FLAG_SUPPORTS_RENAME;
        }
        flags = flags | DocumentsContract.Document.FLAG_SUPPORTS_DELETE
                | DocumentsContract.Document.FLAG_SUPPORTS_REMOVE;
        var curorRowBuilder = cursor.newRow().add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, localDocumentId)
                .add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, localFile.equals(baseDirectory) ? applicationName : localFile.getName())
                .add(DocumentsContract.Document.COLUMN_SIZE, localFile.length())
                .add(DocumentsContract.Document.COLUMN_MIME_TYPE, getTypeForFile(localFile))
                .add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, localFile.lastModified())
                .add(DocumentsContract.Document.COLUMN_FLAGS, flags);
        if (localFile.equals(baseDirectory)) {
            curorRowBuilder.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher);
        }
    }

    private String getTypeForFile(File file) {
        if (file.isDirectory()) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        }
        return getTypeForName(file.getName());
    }

    private String getTypeForName(String name) {
        var lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            var extension = name.substring(lastDot + 1);
            var mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octect-stream";
    }

    private File getFile(String documentId) throws FileNotFoundException {
        Objects.requireNonNull(documentId);
        if (documentId.startsWith(ROOT_ID)) {
            var file = resolve(baseDirectory, documentId.substring(ROOT_ID.length() + 1));
            if (!file.exists()) {
                throw new FileNotFoundException(file.getAbsolutePath() + " " + documentId + " not found");
            }
            return file;
        } else {
            throw new FileNotFoundException(documentId + " is not in any known root");
        }
    }

    private String getDocumentId(File file) {
        return ROOT_ID + "/" + baseDirectory.toPath().relativize(file.toPath()).toString();
    }
}
