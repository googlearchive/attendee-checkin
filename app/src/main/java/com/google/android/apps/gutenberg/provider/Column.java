/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.gutenberg.provider;

/**
 * Encapsulates a database column.
 */
public class Column {

    /**
     * The type description for "_ID" column.
     */
    public static final String _ID_TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT";

    /**
     * The name of this column.
     */
    public final String name;

    /**
     * The type of this column.
     */
    public final String type;

    /**
     * The default value of this column.
     */
    public final String defaultValue;

    /**
     * Creates a new instance of {@link Column}.
     *
     * @param name The name of this column
     * @param type The type of this column
     */
    public Column(String name, String type) {
        this(name, type, null);
    }

    /**
     * Creates a new instance of {@link Column}.
     *
     * @param name         The name of this column
     * @param type         The type of this column
     * @param defaultValue The default value of this column
     */
    public Column(String name, String type, String defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

}
