/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.dataframe.persistence;

import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.xpack.core.dataframe.DataFrameMessages;
import org.elasticsearch.xpack.core.dataframe.transforms.DataFrameTransformCheckpoint;
import org.elasticsearch.xpack.core.dataframe.transforms.DataFrameTransformCheckpointTests;
import org.elasticsearch.xpack.core.dataframe.transforms.DataFrameTransformConfig;
import org.elasticsearch.xpack.core.dataframe.transforms.DataFrameTransformConfigTests;
import org.junit.Before;

public class DataFrameTransformsConfigManagerTests extends DataFrameSingleNodeTestCase {

    private DataFrameTransformsConfigManager transformsConfigManager;

    @Before
    public void createComponents() {
        transformsConfigManager = new DataFrameTransformsConfigManager(client(), xContentRegistry());
    }

    public void testGetMissingTransform() throws InterruptedException {
        // the index does not exist yet
        assertAsync(listener -> transformsConfigManager.getTransformConfiguration("not_there", listener), (DataFrameTransformConfig) null,
                null, e -> {
                    assertEquals(ResourceNotFoundException.class, e.getClass());
                    assertEquals(DataFrameMessages.getMessage(DataFrameMessages.REST_DATA_FRAME_UNKNOWN_TRANSFORM, "not_there"),
                            e.getMessage());
                });

        // create one transform and test with an existing index
        assertAsync(
                listener -> transformsConfigManager
                        .putTransformConfiguration(DataFrameTransformConfigTests.randomDataFrameTransformConfig(), listener),
                true, null, null);

        // same test, but different code path
        assertAsync(listener -> transformsConfigManager.getTransformConfiguration("not_there", listener), (DataFrameTransformConfig) null,
                null, e -> {
                    assertEquals(ResourceNotFoundException.class, e.getClass());
                    assertEquals(DataFrameMessages.getMessage(DataFrameMessages.REST_DATA_FRAME_UNKNOWN_TRANSFORM, "not_there"),
                            e.getMessage());
                });
    }

    public void testDeleteMissingTransform() throws InterruptedException {
        // the index does not exist yet
        assertAsync(listener -> transformsConfigManager.deleteTransform("not_there", listener), (Boolean) null, null, e -> {
            assertEquals(ResourceNotFoundException.class, e.getClass());
            assertEquals(DataFrameMessages.getMessage(DataFrameMessages.REST_DATA_FRAME_UNKNOWN_TRANSFORM, "not_there"), e.getMessage());
        });

        // create one transform and test with an existing index
        assertAsync(
                listener -> transformsConfigManager
                        .putTransformConfiguration(DataFrameTransformConfigTests.randomDataFrameTransformConfig(), listener),
                true, null, null);

        // same test, but different code path
        assertAsync(listener -> transformsConfigManager.deleteTransform("not_there", listener), (Boolean) null, null, e -> {
            assertEquals(ResourceNotFoundException.class, e.getClass());
            assertEquals(DataFrameMessages.getMessage(DataFrameMessages.REST_DATA_FRAME_UNKNOWN_TRANSFORM, "not_there"), e.getMessage());
        });
    }

    public void testCreateReadDeleteTransform() throws InterruptedException {
        DataFrameTransformConfig transformConfig = DataFrameTransformConfigTests.randomDataFrameTransformConfig();

        // create transform
        assertAsync(listener -> transformsConfigManager.putTransformConfiguration(transformConfig, listener), true, null, null);

        // read transform
        assertAsync(listener -> transformsConfigManager.getTransformConfiguration(transformConfig.getId(), listener), transformConfig, null,
                null);

        // try to create again
        assertAsync(listener -> transformsConfigManager.putTransformConfiguration(transformConfig, listener), (Boolean) null, null, e -> {
            assertEquals(ResourceAlreadyExistsException.class, e.getClass());
            assertEquals(DataFrameMessages.getMessage(DataFrameMessages.REST_PUT_DATA_FRAME_TRANSFORM_EXISTS, transformConfig.getId()),
                    e.getMessage());
        });

        // delete transform
        assertAsync(listener -> transformsConfigManager.deleteTransform(transformConfig.getId(), listener), true, null, null);

        // delete again
        assertAsync(listener -> transformsConfigManager.deleteTransform(transformConfig.getId(), listener), (Boolean) null, null, e -> {
            assertEquals(ResourceNotFoundException.class, e.getClass());
            assertEquals(DataFrameMessages.getMessage(DataFrameMessages.REST_DATA_FRAME_UNKNOWN_TRANSFORM, transformConfig.getId()),
                    e.getMessage());
        });

        // try to get deleted transform
        assertAsync(listener -> transformsConfigManager.getTransformConfiguration(transformConfig.getId(), listener),
                (DataFrameTransformConfig) null, null, e -> {
                    assertEquals(ResourceNotFoundException.class, e.getClass());
                    assertEquals(DataFrameMessages.getMessage(DataFrameMessages.REST_DATA_FRAME_UNKNOWN_TRANSFORM, transformConfig.getId()),
                            e.getMessage());
                });
    }

    public void testCreateReadDeleteCheckPoint() throws InterruptedException {
        DataFrameTransformCheckpoint checkpoint = DataFrameTransformCheckpointTests.randomDataFrameTransformCheckpoints();

        // create
        assertAsync(listener -> transformsConfigManager.putTransformCheckpoint(checkpoint, listener), true, null, null);

        // read
        assertAsync(listener -> transformsConfigManager.getTransformCheckpoint(checkpoint.getTransformId(), checkpoint.getCheckpoint(),
                listener), checkpoint, null, null);

        // delete
        assertAsync(listener -> transformsConfigManager.deleteTransform(checkpoint.getTransformId(), listener), true, null, null);

        // delete again
        assertAsync(listener -> transformsConfigManager.deleteTransform(checkpoint.getTransformId(), listener), (Boolean) null, null, e -> {
            assertEquals(ResourceNotFoundException.class, e.getClass());
            assertEquals(DataFrameMessages.getMessage(DataFrameMessages.REST_DATA_FRAME_UNKNOWN_TRANSFORM, checkpoint.getTransformId()),
                    e.getMessage());
        });

        // getting a non-existing checkpoint returns null
        assertAsync(listener -> transformsConfigManager.getTransformCheckpoint(checkpoint.getTransformId(), checkpoint.getCheckpoint(),
                listener), DataFrameTransformCheckpoint.EMPTY, null, null);
    }
}
