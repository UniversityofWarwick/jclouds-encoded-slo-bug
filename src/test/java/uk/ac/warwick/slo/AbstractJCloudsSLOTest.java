package uk.ac.warwick.slo;

import com.google.common.io.ByteSource;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.MultipartPart;
import org.jclouds.blobstore.domain.MultipartUpload;
import org.jclouds.blobstore.options.PutOptions;
import org.jclouds.blobstore.strategy.internal.MultipartUploadSlicingAlgorithm;
import org.jclouds.io.Payload;
import org.jclouds.io.PayloadSlicer;
import org.jclouds.io.internal.BasePayloadSlicer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.assertEquals;

abstract class AbstractJCloudsSLOTest {

    private final PayloadSlicer payloadSlicer = new BasePayloadSlicer();

    private final String containerName = "slo-test";
    private final ByteSource in = ByteSource.wrap(new byte[38547913]);

    abstract BlobStoreContext getContext();

    @Before
    public void setup() throws Exception {
        if (!getContext().getBlobStore().containerExists(containerName))
            getContext().getBlobStore().createContainerInLocation(null, containerName);
    }

    @After
    public void tearDown() throws Exception {
        if (getContext().getBlobStore().containerExists(containerName))
            getContext().getBlobStore().deleteContainer(containerName);
    }

    private void putSLO(ByteSource in, String key) throws Exception {
        BlobStore blobStore = getContext().getBlobStore();

        long size = in.size();
        Blob blob =
            blobStore.blobBuilder(key)
                .payload(in)
                .contentDisposition(key)
                .contentLength(size)
                .build();

        long partSize =
            new MultipartUploadSlicingAlgorithm(blobStore.getMinimumMultipartPartSize(), blobStore.getMaximumMultipartPartSize(), blobStore.getMaximumNumberOfParts())
                .calculateChunkSize(size);

        MultipartUpload multipartUpload = blobStore.initiateMultipartUpload(containerName, blob.getMetadata(), PutOptions.NONE);

        List<MultipartPart> results = new ArrayList<>();

        int i = 0;
        for (Payload payload : payloadSlicer.slice(blob.getPayload(), partSize)) {
            final int index = ++i;
            results.add(blobStore.uploadMultipartPart(multipartUpload, index, payload));
        }

        List<MultipartPart> parts =
            results.stream()
                .sorted(comparing(MultipartPart::partNumber))
                .collect(toList());

        blobStore.completeMultipartUpload(multipartUpload, parts);
    }

    private void assertCanPutAndGetSLO(String key) throws Exception {
        putSLO(in, key);

        Blob blob = getContext().getBlobStore().getBlob(containerName, key);
        assertEquals(38547913L, blob.getMetadata().getSize().longValue());
    }

    @Test
    public void sloNoSpaces() throws Exception {
        String key = "Files/OpenOffice.org3.3/openofficeorg1.cab";

        assertCanPutAndGetSLO(key);
    }

    @Test
    public void sloWithSpaces() throws Exception {
        String key = "Files/OpenOffice.org 3.3 (en-GB) Installation Files/openofficeorg1.cab";

        assertCanPutAndGetSLO(key);
    }

    @Test
    public void sloWithEncodedChars() throws Exception {
        String key = "Files/OpenOffice.org 3.3 %28en-GB%29 Installation Files/openofficeorg1.cab";

        assertCanPutAndGetSLO(key);
    }
}
