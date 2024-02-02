/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.microsphere.logging.log4j2;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.io.Serializable;
import java.util.Map;

/**
 * Delegating {@link Layout}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Layout
 * @since 1.0.0
 */
public class DelegatingLayout<T extends Serializable> implements Layout<T> {

    private Layout<T> delegate;

    public DelegatingLayout(Layout<T> delegate) {
        this.delegate = delegate;
    }

    public DelegatingLayout() {
    }

    @Override
    public byte[] getFooter() {
        return delegate.getFooter();
    }

    @Override
    public byte[] getHeader() {
        return delegate.getHeader();
    }

    @Override
    public byte[] toByteArray(LogEvent event) {
        return delegate.toByteArray(event);
    }

    @Override
    public T toSerializable(LogEvent event) {
        return delegate.toSerializable(event);
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public Map<String, String> getContentFormat() {
        return delegate.getContentFormat();
    }

    @Override
    public void encode(LogEvent source, ByteBufferDestination destination) {
        delegate.encode(source, destination);
    }

    public Layout<T> getDelegate() {
        return delegate;
    }

    public void setDelegate(Layout<T> delegate) {
        this.delegate = delegate;
    }
}
