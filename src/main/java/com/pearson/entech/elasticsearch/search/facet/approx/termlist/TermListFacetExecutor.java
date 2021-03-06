package com.pearson.entech.elasticsearch.search.facet.approx.termlist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.CacheRecycler;
import org.elasticsearch.common.trove.set.hash.THashSet;
import org.elasticsearch.index.fielddata.BytesValues;
import org.elasticsearch.index.fielddata.BytesValues.Iter;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.search.facet.FacetExecutor;
import org.elasticsearch.search.facet.InternalFacet;

public class TermListFacetExecutor extends FacetExecutor {

    int _maxPerShard;
    String _facetName;
    IndexFieldData<?> _indexFieldData;

    List<String> _entries = new ArrayList<String>();
    final THashSet<String> _hashEntries = CacheRecycler.popHashSet();

    public TermListFacetExecutor(final IndexFieldData<?> indexFieldData, final String facetName, final int maxPerShard) {
        _maxPerShard = maxPerShard;
        _facetName = facetName;
        _indexFieldData = indexFieldData;
    }

    @Override
    public InternalFacet buildFacet(final String facetName) {
        return new InternalStringTermListFacet(facetName, _entries);
    }

    @Override
    public Collector collector() {
        return new Collector(_maxPerShard);
    }

    final class Collector extends FacetExecutor.Collector {

        private BytesValues values;

        Collector(final int maxPerShard) {

        }

        @Override
        public void setNextReader(final AtomicReaderContext context) throws IOException {
            values = _indexFieldData.load(context).getBytesValues();
        }

        protected void onValue(final int docId, final BytesRef value, final int hashCode, final BytesValues values) {
            _entries.add(value.utf8ToString());
        }

        @Override
        public void collect(final int docId) throws IOException {

            if(_entries.size() > _maxPerShard)
                return;

            if(values.hasValue(docId)) {
                final Iter iter = values.getIter(docId);
                while(iter.hasNext() && _entries.size()<_maxPerShard) {
                    onValue(docId, iter.next(), iter.hash(), values);
                }
            }

        }

        @Override
        public void postCollection() {
            CacheRecycler.pushHashSet(_hashEntries);
        }
    }

}
