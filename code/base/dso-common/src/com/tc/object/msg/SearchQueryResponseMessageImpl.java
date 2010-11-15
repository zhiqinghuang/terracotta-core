/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.NVPair;
import com.tc.object.session.SessionID;
import com.tc.search.IndexQueryResult;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 */
public class SearchQueryResponseMessageImpl extends DSOMessageBase implements SearchQueryResponseMessage {

  private final static byte      SEARCH_REQUEST_ID       = 0;
  private final static byte      RESULTS_SIZE            = 1;
  private final static byte      AGGREGATOR_RESULTS_SIZE = 2;

  private SearchRequestID        requestID;
  private List<IndexQueryResult> results;
  private List<NVPair>           aggregatorResults;

  public SearchQueryResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                        MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public SearchQueryResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                        TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  /**
   * {@inheritDoc}
   */
  public void initialSearchResponseMessage(SearchRequestID searchRequestID, List<IndexQueryResult> searchResults,
                                           List<NVPair> aggregators) {
    this.requestID = searchRequestID;
    this.results = searchResults;
    this.aggregatorResults = aggregators;
  }

  /**
   * {@inheritDoc}
   */
  public SearchRequestID getRequestID() {
    return this.requestID;
  }

  /**
   * {@inheritDoc}
   */
  public List<IndexQueryResult> getResults() {
    return this.results;
  }

  /**
   * {@inheritDoc}
   */
  public List<NVPair> getAggregatorResults() {
    return aggregatorResults;
  }

  @Override
  protected void dehydrateValues() {
    final TCByteBufferOutputStream outStream = getOutputStream();

    putNVPair(SEARCH_REQUEST_ID, this.requestID.toLong());
    putNVPair(RESULTS_SIZE, this.results.size());
    int count = 0;

    for (IndexQueryResult result : this.results) {
      // TODO: does the key need to be encoded?
      result.serializeTo(outStream);
      count++;
    }
    Assert.assertEquals(this.results.size(), count);

    putNVPair(AGGREGATOR_RESULTS_SIZE, this.aggregatorResults.size());
    count = 0;

    for (NVPair result : this.aggregatorResults) {
      // TODO: does the key need to be encoded?
      result.serializeTo(outStream);
      count++;
    }
    Assert.assertEquals(this.aggregatorResults.size(), count);

  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    TCByteBufferInput input = getInputStream();

    switch (name) {
      case SEARCH_REQUEST_ID:
        this.requestID = new SearchRequestID(getLongValue());
        return true;

      case RESULTS_SIZE:
        int size = getIntValue();
        this.results = new ArrayList((int) (size * 1.5));
        while (size-- > 0) {
          // TODO: Do we need to decode?
          IndexQueryResult result = new IndexQueryResultImpl();
          result.deserializeFrom(input);
          this.results.add(result);
        }
        return true;

      case AGGREGATOR_RESULTS_SIZE:
        int aggregatorSize = getIntValue();
        this.aggregatorResults = new ArrayList((int) (aggregatorSize * 1.5));
        while (aggregatorSize-- > 0) {

          NVPair pair = AbstractNVPair.deserializeInstance(input);
          this.aggregatorResults.add(pair);
        }
        return true;
      default:
        return false;
    }

  }

}