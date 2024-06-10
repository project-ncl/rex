/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.api.openapi;

public class OpenapiConstants {
    public static final String SUCCESS_DESCRIPTION = "Success with results";
    public static final String SUCCESS_CODE = "200";
    public static final String ENTITY_CREATED_DESCRIPTION = "Entity successfully created";
    public static final String ENTITY_CREATED_CODE = "201";
    public static final String ACCEPTED_DESCRIPTION = "Request was accepted for processing";
    public static final String ACCEPTED_CODE = "202";
    public static final String ENTITY_UPDATED_DESCRIPTION = "Entity successfully updated";
    public static final String ENTITY_UPDATED_CODE = "204";
    public static final String ENTITY_DELETED_DESCRIPTION = "Entity deleted";
    public static final String ENTITY_DELETED_CODE = "204";
    public static final String NO_CONTENT_DESCRIPTION = "Success but no content provided";
    public static final String NO_CONTENT_CODE = "204";
    public static final String INVALID_DESCRIPTION = "Invalid input parameters or validation error";
    public static final String INVALID_CODE = "400";
    public static final String FORBIDDEN_DESCRIPTION = "User must be logged in.";
    public static final String FORBIDDEN_CODE = "403";
    public static final String MOVED_TEMPORARILY_DESCRIPTION = "Redirected to resource";
    public static final String MOVED_TEMPORARILY_CODE = "302";
    public static final String NOT_FOUND_DESCRIPTION = "Can not find specified result";
    public static final String NOT_FOUND_CODE = "404";
    public static final String CONFLICTED_DESCRIPTION = "Conflict while saving an entity";
    public static final String CONFLICTED_CODE = "409";
    public static final String SERVER_ERROR_DESCRIPTION = "Server error";
    public static final String SERVER_ERROR_CODE = "500";

}
