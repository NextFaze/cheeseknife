package com.nextfaze.cheeseknife;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class CheeseKnifeException extends RuntimeException {

    @Getter
    private int mResourceId = 0;

    @Getter
    private Object mTarget;

    @Getter
    private Class mHandler;

    @Getter
    private String mResourceName;

    public CheeseKnifeException(Object target, int resourceId) {
        mTarget = target;
        mResourceId = resourceId;
    }

    public CheeseKnifeException(Object target, String name) {
        mTarget = target;
        mResourceName = name;
    }

    public CheeseKnifeException(Object target, String name, Class annotationClass) {
        this(target, name);
        mHandler = annotationClass;
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder("could not find resource while binding ");
        builder.append(mTarget);

        if(mHandler != null) {
            builder.append(" with handler");
            builder.append(mHandler);
        } else {
            builder.append(" with view");
            if(mResourceName != null) {
                builder.append(", resource name matching ");
                builder.append(mResourceName);
            }
            else if(mResourceId > 0) {
                builder.append(", resource id ");
                builder.append(mResourceId);
            }
        }
        return builder.toString();
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }
}
