/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.extension;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionDescription;
import org.geysermc.geyser.api.extension.exception.InvalidExtensionException;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class GeyserExtensionClassLoader extends URLClassLoader {
    private final GeyserExtensionLoader loader;
    private final Object2ObjectMap<String, Class<?>> classes = new Object2ObjectOpenHashMap<>();

    public GeyserExtensionClassLoader(GeyserExtensionLoader loader, ClassLoader parent, Path path) throws MalformedURLException {
        super(new URL[] { path.toUri().toURL() }, parent);
        this.loader = loader;

        super.addURL(getClass().getProtectionDomain().getCodeSource().getLocation());
//        super.addURL(URI.create(getClass().getProtectionDomain().getCodeSource().getLocation().toString() + "!/" + ("org.geysermc.geyser.platform." + GeyserImpl.getInstance().getPlatformType().toString().toLowerCase() + ".shaded.").replace('.', '/')).toURL());
    }

    public Extension load(ExtensionDescription description) throws InvalidExtensionException {
        try {
            Class<?> jarClass;
            try {
                jarClass = Class.forName(description.main(), true, this);
            } catch (ClassNotFoundException ex) {
                throw new InvalidExtensionException("Class " + description.main() + " not found, extension cannot be loaded", ex);
            }

            Class<? extends Extension> extensionClass;
            try {
                extensionClass = jarClass.asSubclass(Extension.class);
            } catch (ClassCastException ex) {
                throw new InvalidExtensionException("Main class " + description.main() + " should implement Extension, but extends " + jarClass.getSuperclass().getSimpleName(), ex);
            }

            return extensionClass.getConstructor().newInstance();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new InvalidExtensionException("No public constructor", ex);
        } catch (InstantiationException ex) {
            throw new InvalidExtensionException("Abnormal extension type", ex);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return this.findClass(name, true);
    }

    protected Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        if (name.startsWith("org.geysermc.geyser.") || name.startsWith("net.minecraft.")) {
//            throw new ClassNotFoundException(name);
        }

        Class<?> result = this.classes.get(name);
        if (result == null) {
            try {
                result = super.findClass(name);
            } catch (ClassNotFoundException ignored) {
                System.out.println("");
            }

            if (result == null && checkGlobal) {
                result = this.loader.classByName(name);

                if (result == null) {
                    // Try get the class from the platform relocation
                    try {
                        String relocatedName = "org.geysermc.geyser.platform." + GeyserImpl.getInstance().getPlatformType().toString().toLowerCase() + ".shaded." + name;
                        String relocatedNamePath = relocatedName.replace('.', '/');
                        String namePath = name.replace('.', '/');
                        GeyserImpl.getInstance().getLogger().debug("Trying to load " + name + " from platform relocation (" + relocatedName + ")");

                        // Load the relocated class to check it exists
//                        Class<?> clazz = Class.forName(relocatedName);

                        // Create the new class
//                        ClassWriter cw = new ClassWriter(0);
//                        cw.visit(Opcodes.V16, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, namePath, null, relocatedNamePath, null);

                        // Clone the constructors
//                        for (Constructor<?> constructor : clazz.getConstructors()) {
//                            String discriptor = Type.getConstructorDescriptor(constructor);
//                            MethodVisitor mv = cw.visitMethod(constructor.getModifiers(), "<init>", discriptor, null, null);
//                            mv.visitCode();
//                            mv.visitVarInsn(Opcodes.ALOAD, 0);
//                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, relocatedNamePath, "<init>", discriptor, false);
//                            mv.visitInsn(Opcodes.RETURN);
//                            mv.visitMaxs(1, 1 + constructor.getParameterCount());
//                            mv.visitEnd();
//                        }

//                        cw.visitEnd();


                        ClassReader reader = new ClassReader(GeyserExtensionClassLoader.class.getClassLoader().getResourceAsStream(relocatedNamePath + ".class"));
                        ClassWriter writer = new ClassWriter(0);

                        ClassVisitor visitor = new ClassRemapper(writer, new SimpleRemapper(relocatedNamePath, namePath));

                        reader.accept(visitor, 0);

                        byte[] bytes = writer.toByteArray();
                        result = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException e) {
                        // We don't need to do anything here, we will just return null
                        System.out.println("");
                    }
                }
            }

            if (result != null) {
                this.loader.setClass(name, result);
            }

            this.classes.put(name, result);
        }
        return result;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }
}
