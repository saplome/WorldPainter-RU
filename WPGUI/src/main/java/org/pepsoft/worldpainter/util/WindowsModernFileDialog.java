package org.pepsoft.worldpainter.util;

import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the Windows Vista+ IFileDialog based picker supplied by JnaFileChooser.
 * Reflection keeps the rest of WorldPainter platform-neutral and allows a clean fallback
 * if the native integration cannot be initialised on a particular Windows installation.
 */
final class WindowsModernFileDialog {
    private WindowsModernFileDialog() {
    }

    static File selectFile(Window parent, String title, File fileOrDir, FileFilter filter, boolean save) {
        final File[] files = selectFiles(parent, title, fileOrDir, filter, false, save);
        return ((files != null) && (files.length == 1)) ? files[0] : null;
    }

    static File[] selectFiles(Window parent, String title, File fileOrDir, FileFilter filter, boolean multiple, boolean save) {
        try {
            final Class<?> chooserClass = Class.forName("jnafilechooser.api.JnaFileChooser");
            final Object chooser = chooserClass.getConstructor().newInstance();

            invokeOptional(chooser, "setTitle", title);
            invokeOptional(chooser, "setDialogTitle", title);
            configureFilesMode(chooser);
            configureInitialSelection(chooser, fileOrDir);
            configureFilter(chooser, filter);

            if (multiple && (! invokeOptional(chooser, "setMultiSelectionEnabled", true))) {
                invokeOptional(chooser, "setMultiSelection", true);
            }

            final boolean approved;
            if (save) {
                approved = invokeDialog(chooser, parent, "showSaveDialog");
            } else if (hasMethod(chooser.getClass(), "showOpenDialog")) {
                approved = invokeDialog(chooser, parent, "showOpenDialog");
            } else {
                approved = invokeDialog(chooser, parent, "showDialog");
            }
            if (! approved) {
                return null;
            }

            if (multiple) {
                final Object selectedFiles = invokeRequiredNoArgs(chooser, "getSelectedFiles");
                if (selectedFiles instanceof File[]) {
                    final File[] files = (File[]) selectedFiles;
                    return (files.length > 0) ? files : null;
                }
            }
            final Object selectedFile = invokeRequiredNoArgs(chooser, "getSelectedFile");
            return (selectedFile instanceof File) ? new File[] {(File) selectedFile} : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new IllegalStateException("Could not open the modern Windows file dialog", unwrap(e));
        }
    }

    private static void configureFilesMode(Object chooser) throws ReflectiveOperationException {
        for (Class<?> nestedClass: chooser.getClass().getDeclaredClasses()) {
            if (nestedClass.isEnum() && nestedClass.getSimpleName().equals("Mode")) {
                for (Object value: nestedClass.getEnumConstants()) {
                    if (((Enum<?>) value).name().equalsIgnoreCase("Files")) {
                        invokeOptional(chooser, "setMode", value);
                        return;
                    }
                }
            }
        }
    }

    private static void configureInitialSelection(Object chooser, File fileOrDir) throws ReflectiveOperationException {
        if (fileOrDir == null) {
            return;
        }
        final File directory = fileOrDir.isDirectory() ? fileOrDir : fileOrDir.getAbsoluteFile().getParentFile();
        if (directory != null) {
            if (! invokeOptional(chooser, "setCurrentDirectory", directory)) {
                invokeOptional(chooser, "setInitialDirectory", directory);
            }
        }
        if (! fileOrDir.isDirectory()) {
            if (! invokeOptional(chooser, "setSelectedFile", fileOrDir)) {
                if (! invokeOptional(chooser, "setDefaultFileName", fileOrDir.getName())) {
                    invokeOptional(chooser, "setDefaultFilename", fileOrDir.getName());
                }
            }
        }
    }

    private static void configureFilter(Object chooser, FileFilter filter) throws ReflectiveOperationException {
        if (filter == null) {
            return;
        }
        final List<String> extensions = new ArrayList<>();
        for (String item: filter.getExtensions().split(";")) {
            String extension = item.trim();
            while (extension.startsWith("*")) {
                extension = extension.substring(1);
            }
            if (extension.startsWith(".")) {
                extension = extension.substring(1);
            }
            if (! extension.isEmpty()) {
                extensions.add(extension);
            }
        }
        if (extensions.isEmpty()) {
            extensions.add("*");
        }
        final Method addFilter = chooser.getClass().getMethod("addFilter", String.class, String[].class);
        addFilter.invoke(chooser, filter.getDescription(), extensions.toArray(new String[0]));
    }

    private static boolean invokeDialog(Object chooser, Component parent, String methodName) throws ReflectiveOperationException {
        Method candidate = null;
        for (Method method: chooser.getClass().getMethods()) {
            if (method.getName().equals(methodName) && (method.getParameterCount() == 1)
                    && ((parent == null) || method.getParameterTypes()[0].isInstance(parent)
                    || method.getParameterTypes()[0].isAssignableFrom(Component.class))) {
                candidate = method;
                break;
            }
        }
        if (candidate == null) {
            throw new NoSuchMethodException(methodName);
        }
        final Object result = candidate.invoke(chooser, parent);
        return (result instanceof Boolean) && (Boolean) result;
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (Method method: type.getMethods()) {
            if (method.getName().equals(name) && (method.getParameterCount() == 1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean invokeOptional(Object target, String name, Object argument) throws ReflectiveOperationException {
        for (Method method: target.getClass().getMethods()) {
            if (method.getName().equals(name) && (method.getParameterCount() == 1)
                    && isCompatible(method.getParameterTypes()[0], argument)) {
                method.invoke(target, argument);
                return true;
            }
        }
        return false;
    }

    private static Object invokeRequiredNoArgs(Object target, String name) throws ReflectiveOperationException {
        return target.getClass().getMethod(name).invoke(target);
    }

    private static boolean isCompatible(Class<?> parameterType, Object value) {
        if (value == null) {
            return ! parameterType.isPrimitive();
        }
        if (parameterType.isInstance(value)) {
            return true;
        }
        return (parameterType == boolean.class) && (value instanceof Boolean);
    }

    private static Throwable unwrap(Throwable throwable) {
        if ((throwable instanceof InvocationTargetException) && (((InvocationTargetException) throwable).getCause() != null)) {
            return ((InvocationTargetException) throwable).getCause();
        }
        return throwable;
    }
}
