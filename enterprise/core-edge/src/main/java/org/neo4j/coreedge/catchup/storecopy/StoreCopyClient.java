/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge.catchup.storecopy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.neo4j.coreedge.catchup.CoreClient;
import org.neo4j.coreedge.identity.MemberId;
import org.neo4j.coreedge.identity.StoreId;

public class StoreCopyClient
{
    private final CoreClient coreClient;

    public StoreCopyClient( CoreClient coreClient )
    {
        this.coreClient = coreClient;
    }

    long copyStoreFiles( MemberId from, StoreFileStreams storeFileStreams ) throws StoreCopyFailedException
    {
        coreClient.setStoreFileStreams( storeFileStreams );

        CompletableFuture<Long> txId = new CompletableFuture<>();
        StoreFileStreamingCompleteListener fileStreamingCompleteListener = txId::complete;

        coreClient.addStoreFileStreamingCompleteListener( fileStreamingCompleteListener );

        try
        {
            coreClient.requestStore( from );
            return txId.get();
        }
        catch ( InterruptedException | ExecutionException e )
        {
            throw new StoreCopyFailedException( e );
        }
        finally
        {
            coreClient.removeStoreFileStreamingCompleteListener( fileStreamingCompleteListener );
        }
    }

    StoreId fetchStoreId( MemberId from ) throws StoreIdDownloadFailedException
    {
        CompletableFuture<StoreId> storeIdCompletableFuture = new CompletableFuture<>();
        coreClient.setStoreIdConsumer( storeIdCompletableFuture::complete );
        coreClient.requestStoreId( from );
        try
        {
            return storeIdCompletableFuture.get();
        }
        catch ( InterruptedException e )
        {
            Thread.interrupted();
            throw new StoreIdDownloadFailedException( e );
        }
        catch ( ExecutionException e )
        {
            throw new StoreIdDownloadFailedException( e );
        }
    }
}
