/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
 * %%
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
 * #L%
 */
package org.wildfly.camel.test.common.aws;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class S3Utils {

    public static final String BUCKET_NAME = "wfc-aws-s3-bucket";
    
    // Attach Policy: AmazonS3FullAccess
    public static AmazonS3Client createS3Client() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonS3Client client = !credentials.isValid() ? null : (AmazonS3Client) 
                AmazonS3ClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1")
                .build();
        return client;
    }

    public static void addRoutes(CamelContext camelctx) throws Exception {
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String clientref = "amazonS3Client=#s3Client";
                from("direct:upload").to("aws-s3://" + BUCKET_NAME + "?" + clientref);
                from("aws-s3://" + BUCKET_NAME + "?" + clientref).to("seda:read");
            }
        });
    }

    public static void createBucket(AmazonS3Client client) {
        client.createBucket(BUCKET_NAME);
    }

    public static void deleteBucket(AmazonS3Client client) {
        client.deleteBucket(BUCKET_NAME);
    }

}
