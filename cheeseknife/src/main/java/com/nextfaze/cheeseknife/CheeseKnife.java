package com.nextfaze.cheeseknife;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.view.View;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
public final class CheeseKnife {

    private CheeseKnife() {
        throw new AssertionError("No instances.");
    }

    /**
     * Inject annotated fields and methods in the specified {@link Activity}. The current content
     * view is used as the view root.
     *
     * @param target Target activity for field injection.
     */
    public static void inject(Activity target) {
        inject(target, target, Finder.ACTIVITY);
    }

    /**
     * Inject annotated fields and methods in the specified {@link View}. The view and its children
     * are used as the view root.
     *
     * @param target Target view for field injection.
     */
    public static void inject(View target) {
        inject(target, target, Finder.VIEW);
    }

    /**
     * Inject annotated fields and methods in the specified {@link Dialog}. The current content
     * view is used as the view root.
     *
     * @param target Target dialog for field injection.
     */
    public static void inject(Dialog target) {
        inject(target, target, Finder.DIALOG);
    }

    /**
     * Inject annotated fields and methods in the specified {@code target} using the {@code source}
     * {@link Activity} as the view root.
     *
     * @param target Target class for field injection.
     * @param source Activity on which IDs will be looked up.
     */
    public static void inject(Object target, Activity source) {
        inject(target, source, Finder.ACTIVITY);
    }

    /**
     * Inject annotated fields and methods in the specified {@code target} using the {@code source}
     * {@link View} as the view root.
     *
     * @param target Target class for field injection.
     * @param source View root on which IDs will be looked up.
     */
    public static void inject(Object target, View source) {
        inject(target, source, Finder.VIEW);
    }

    /**
     * Inject annotated fields and methods in the specified {@code target} using the {@code source}
     * {@link Dialog} as the view root.
     *
     * @param target Target class for field injection.
     * @param source Dialog on which IDs will be looked up.
     */
    public static void inject(Object target, Dialog source) {
        inject(target, source, Finder.DIALOG);
    }

    static void inject(final Object target, Object source, Finder finder) throws CheeseKnifeException {
        log.debug("injecting views in {}", target);

        // do not inject views in edit mode
        if(target instanceof View) {
            View targetView = (View) target;
            if(targetView.isInEditMode())
                return;
        }

        for (Field field : getAnnotatedFields(target, Bind.class)) {
            Bind annotation = field.getAnnotation(Bind.class);
            View view = null;

            // find view by id first
            int viewId = annotation.id();
            if(viewId > 0) {
                view = finder.findOptionalView(source, viewId);
            }
            if(view == null) {
                // find view from string
                String name = annotation.name();
                if (name == null || name.isEmpty()) {
                    // infer resource name from field name
                    name = camelize(field.getName());
                }

                view = finder.findOptionalViewByName(source, name);

                if(view == null && field.getAnnotation(Optional.class) == null) {
                    // could not find view, throw an exception
                    log.debug("no optional annotation on {}", field);
                    throw new CheeseKnifeException(target, name);
                }
            }

            if(view != null)
                setFieldValue(field, target, view);
        }

        log.debug("injecting onClick handlers in {}", target);
        injectOnClick(target, source, finder, OnClickAnnotation.ON_CLICK);
        injectOnClick(target, source, finder, OnClickAnnotation.ON_LONG_CLICK);

        log.debug("injection complete");
    }

    public static void reset(Object target) {
        for (Field field : getAnnotatedFields(target, Bind.class)) {
            setFieldValue(field, target, null);
        }
        // TODO: reset onClick handlers ?
    }

    private static void injectOnClick(final Object target, final Object source, Finder finder,
                                      OnClickAnnotation clickAnnotation) {
        Class<? extends Annotation> annotationClass = clickAnnotation.getAnnotationClass();

        for (final Method method : getAnnotatedMethods(target, annotationClass)) {
            Annotation annotation = method.getAnnotation(annotationClass);
            Set<View> views = new HashSet<>();
            boolean isOptional = method.getAnnotation(Optional.class) != null;

            for(int id : clickAnnotation.id(annotation)) {
                if (id > 0) {
                    View view = finder.findOptionalView(source, id);
                    if(view != null) {
                        views.add(view);
                    }
                    else if(!isOptional) {
                        // view not found and injection is non-optional, raise error
                        throw new CheeseKnifeException(target, id);
                    }
                }
            }

            for(String name : clickAnnotation.name(annotation)) {
                if (name == null || name.isEmpty()) {
                    // ignore empty name if already found by id
                    if(!views.isEmpty()) continue;

                    // infer resource name from method name
                    name = camelize(method.getName()
                            .replaceAll("^on", "")
                            .replaceAll("(Press|Click)(ed)?$", ""));
                }

                // find id from string
                View view = finder.findOptionalViewByName(source, name);
                if(view == null && !isOptional) {
                    throw new CheeseKnifeException(target, name, annotationClass);
                }

                if(view != null)
                    views.add(view);
            }

            for(View view : views) {
                // set up onClick handler
                log.debug("setting {} {} -> {}", view.getClass().getSimpleName(), annotationClass.getSimpleName(), method);
                method.setAccessible(true);
                clickAnnotation.setListener(view, target, method);
            }
        }
    }

    private static void onClickHandler(Method method, Object target, View view) {
        Type[] paramTypes = method.getGenericParameterTypes();

        try {
            if(paramTypes.length == 0) {
                method.invoke(target);
            }
            else {
                Object[] params = new Object[paramTypes.length];
                params[0] = view;
                method.invoke(target, params);
            }
        } catch(Throwable e) {
            if(e instanceof InvocationTargetException)
                e = e.getCause();
            log.debug("error invoking onClick handler {}: {}", method, e);
        }
    }

    @NonNull
    private static String camelize(@NonNull String string) {
        return string.replaceAll("(.)(\\p{Upper})", "$1_$2")
                .toLowerCase(Locale.getDefault())
                .replaceFirst("^[ms]_", "");
    }

    private static void setFieldValue(Field field, Object target, Object value) {
        try {
            log.debug("setting: {} -> {}", field.getName(), value);
            field.setAccessible(true);
            field.set(target, value);
        }
        catch(IllegalAccessException e) {
            log.debug("illegal access: {}", e);
        }
    }

    // return a list of fields in target that have Bind annotations
    private static List<Field> getAnnotatedFields(Object target, Class<? extends Annotation> annotationClass) {
        List<Field> list = new ArrayList<>();
        Set<Field> fields = new HashSet<>();
        fields.addAll(Arrays.asList(target.getClass().getFields()));
        fields.addAll(Arrays.asList(target.getClass().getDeclaredFields()));   // includes protected / private

        for (Field field : fields) {
            if(field.isAnnotationPresent(annotationClass))
                list.add(field);
        }
        return list;
    }

    private static List<Method> getAnnotatedMethods(Object target, Class<? extends Annotation> annotationClass) {
        List<Method> list  = new ArrayList<>();
        Set<Method> methods = new HashSet<>();
        methods.addAll(Arrays.asList(target.getClass().getMethods()));
        methods.addAll(Arrays.asList(target.getClass().getDeclaredMethods()));  // includes protected / private

        for (Method method : methods) {
            if(method.isAnnotationPresent(annotationClass))
                list.add(method);
        }

        log.debug("target: {}, annotation: {}, methods: {}", target, annotationClass, list);
        return list;
    }

    private enum OnClickAnnotation {
        ON_CLICK {
            public int[] id(Annotation annotation) { return ((OnClick) annotation).id(); }
            public String[] name(Annotation annotation) {
                return ((OnClick) annotation).name();
            }
            public void setListener(final View view, final Object target, final Method method) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onClickHandler(method, target, v);
                    }
                });
            }
            public Class<? extends Annotation> getAnnotationClass() {
                return OnClick.class;
            }
        },
        ON_LONG_CLICK {
            public int[] id(Annotation annotation) { return ((OnLongClick) annotation).id(); }
            public String[] name(Annotation annotation) {
                return ((OnLongClick) annotation).name();
            }
            public void setListener(final View view, final Object target, final Method method) {
                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        onClickHandler(method, target, v);
                        return true;
                    }
                });
            }
            public Class<? extends Annotation> getAnnotationClass() {
                return OnLongClick.class;
            }
        };

        public abstract void setListener(final View view, final Object target, final Method method);
        public abstract int[] id(Annotation annotation);
        public abstract String[] name(Annotation annotation);
        public abstract Class<? extends Annotation> getAnnotationClass();
    }

    private enum Finder {
        VIEW {
            @Override
            @Nullable
            public View findOptionalView(@NonNull Object source, int id) {
                return ((View) source).findViewById(id);
            }

            @Override
            protected Context getContext(@NonNull Object source) {
                return ((View) source).getContext();
            }
        },
        ACTIVITY {
            @Override
            @Nullable
            public View findOptionalView(@NonNull Object source, int id) {
                return ((Activity) source).findViewById(id);
            }

            @Override
            protected Context getContext(@NonNull Object source) {
                return (Activity) source;
            }
        },
        DIALOG {
            @Override
            @Nullable
            public View findOptionalView(@NonNull Object source, int id) {
                return ((Dialog) source).findViewById(id);
            }

            @Override
            protected Context getContext(@NonNull Object source) {
                return ((Dialog) source).getContext();
            }
        };

        @Nullable
        public abstract View findOptionalView(Object source, int id);

        @Nullable
        public View findOptionalViewByName(Object source, String name) {
            Context context = getContext(source);
            Resources resources = context.getResources();

            List<String> searchPath = new ArrayList<>();
            searchPath.add(name);
            searchPath.add(name.replaceFirst("_view$", ""));

            View view = null;
            for(String searchName : searchPath) {
                int id = resources.getIdentifier(searchName, "id", context.getPackageName());
                if(id == 0) continue;
                view = findOptionalView(source, id);
                if(view != null) break;
            }

            return view;
        }

        protected abstract Context getContext(Object source);
    }

}
