package org.hisp.dhis.commons.collection;

/*
 * Copyright (c) 2004-2015, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.HashMap;
import java.util.concurrent.Callable;

/**
 * Map which allows storing a {@link java.util.concurrent.Callable}
 * and caches its return value on the first call to get(Object, Callable).
 * Subsequent calls returns the cached value.
 *
 * @author Lars Helge Overland
 */
public class CachingMap<K, V>
    extends HashMap<K, V>
{
    /**
     * Returns the cached value if available or executes the Callable and returns
     * the value, which is also cached.
     *
     * @param key the key.
     * @param callable the Callable.
     * @return the return value of the Callable, either from cache or immediate execution.
     */
    public V get( K key, Callable<V> callable )
    {
        V value = super.get( key );

        if ( value == null )
        {
            try
            {
                value = callable.call();
                
                super.put( key, value );
            }
            catch ( Exception ex )
            {
                throw new RuntimeException( ex );
            }
        }
        
        return value;
    }
}
