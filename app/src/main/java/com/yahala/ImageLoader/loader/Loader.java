/**
 * Copyright 2012 Novoda Ltd
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yahala.ImageLoader.loader;

import android.widget.ImageView;


import com.yahala.ImageLoader.OnImageLoadedListener;

import java.lang.ref.WeakReference;

/**
 * Worker class that retrieves images for {@link android.widget.ImageView}s. The information about the image have to be stored in the tag attribute as an
 * {@link com.novoda.imageloader.core.model.ImageTag}
 */
public interface Loader {

    /**
     * Initiates the loading process for the given image view. <code>imageView.getTag()</code> has to be of type
     * {@link com.novoda.imageloader.core.model.ImageTag}
     *
     * @param imageView ImageView with attached image information
     */
    void load(ImageView imageView);

    void setLoadListener(WeakReference<OnImageLoadedListener> onImageLoadedListener);

}
