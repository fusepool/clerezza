/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.clerezza.rdf.cris.lucene;

import org.apache.clerezza.rdf.cris.IndexConstants;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.cris.Condition;
import org.apache.clerezza.rdf.cris.FacetCollector;
import org.apache.clerezza.rdf.cris.PropertyHolder;
import org.apache.clerezza.rdf.cris.ResourceFinder;
import org.apache.clerezza.rdf.cris.ResourceIndexer;
import org.apache.clerezza.rdf.cris.SortSpecification;
import org.apache.clerezza.rdf.cris.VirtualProperty;
import org.apache.clerezza.rdf.cris.WildcardCondition;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an index of RDF resources and provides an interface to search for
 * indexed resources.
 *
 * @author reto, tio, daniel
 */
public class LuceneGraph extends ResourceIndexer implements ResourceFinder {
  
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private int maxHits;
  private Analyzer analyzer;
  private LuceneEngine luceneEngine;

  //private Map<SortFieldArrayWrapper, Sort> sortCache = new HashMap<SortFieldArrayWrapper, Sort>();
  /**
   * Creates a new index.
   *
   * The {@code GraphIndexer} looks for specifications of what properties on
   * what resources to index in the {@code definitionGraph}.
   *
   * The {@code baseGraph} specifies the graph on which the index is built.
   *
   * <p>Notes:
   *
   * <p>
   * This is an expensive operation and it is advisable to call
   * {@link #closeLuceneIndex()} when this instance is no longer needed.
   * </p><p>
   * The GraphIndexer must have write-access to the index directory specified.
   * </p>
   *
   * @param definitionGraph where index definitions are stored
   * @param baseGraph where the resources to index are stored
   * @param indexDirectory The directory where the index is stored.
   * @param createNewIndex Whether to create a new index or reuse an existing
   * one. The constructor does not check if there is a valid exiting index. The
   * user is responsible for setting this value correctly.
   * @param maxHits How many results the indexer returns. All entries in the
   * index are searched, but only @code{maxHits} resources are resolved and
   *		returned in the result.
   *
   * @see IndexDefinitionManager
   */
  public LuceneGraph(TripleCollection definitionGraph,
          TripleCollection baseGraph, Directory indexDirectory,
          boolean createNewIndex, int maxHits) {
    super(definitionGraph, baseGraph, createNewIndex);
    this.maxHits = maxHits;
    analyzer = new StandardAnalyzer(Version.LUCENE_44);

    try {
      luceneEngine = LuceneEngine.getInstance(indexDirectory, analyzer);
    } catch (Exception e) {
      logger.error("Could not create luceneEngine: " + e.getMessage());
    }
  }

  /**
   * Creates a new index with default {@code maxHits}.
   *
   * The {@code GraphIndexer} looks for specifications of what properties on
   * what resources to index in the {@code definitionGraph}.
   *
   * The {@code baseGraph} specifies the graph on which the index is built.
   *
   * <p>Notes:
   *
   * <p>
   * This is an expensive operation and it is advisable to call
   * {@link #closeLuceneIndex()} when this instance is no longer needed.
   * </p><p>
   * The GraphIndexer must have write-access to the index directory specified.
   * </p>
   *
   * @param definitionGraph where index definitions are stored
   * @param baseGraph where the resources to index are stored
   * @param indexDirectory The directory where the index is stored.
   * @param createNewIndex Whether to create a new index or reuse an existing
   * one. The constructor does not check if there is a valid exiting index. The
   * user is responsible for setting this value correctly.
   */
  public LuceneGraph(TripleCollection definitionGraph,
          TripleCollection baseGraph, Directory indexDirectory,
          boolean createNewIndex) {
    this(definitionGraph, baseGraph, indexDirectory, createNewIndex, IndexConstants.DEFAULT_MAXHITS);
  }

  /**
   * Creates a new in-memory index with default {@code maxHits}.
   *
   * The {@code GraphIndexer} looks for specifications of what properties on
   * what resources to index in the {@code definitionGraph}.
   *
   * The {@code baseGraph} specifies the graph on which the index is built.
   *
   * <p>Notes:
   *
   * <p>
   * This is an expensive operation and it is advisable to call
   * {@link #closeLuceneIndex()} when this instance is no longer needed.
   * </p><p>
   * The GraphIndexer must have write-access to the index directory specified.
   * </p>
   *
   * @param definitionGraph where index definitions are stored
   * @param baseGraph where the resources to index are stored
   */
  public LuceneGraph(TripleCollection definitionGraph, TripleCollection baseGraph) throws IOException {
    this(definitionGraph, baseGraph, new RAMDirectory(), true);
    //this(definitionGraph, baseGraph, new NIOFSDirectory(new File("test/")), true);
  }

  /**
   * Returns the Analyzer used by this GraphIndexer instance.
   *
   * @return the Analyzer
   */
  public Analyzer getAnalyzer() {
    return analyzer;
  }

  /**
   * How many results a search on the index returns maximally.
   *
   * @return	the maximum number of results.
   */
  public int getMaxHits() {
    return maxHits;
  }

  /**
   * Set how many results a search on the index returns maximally.
   *
   * @param maxHits	the maximum number of results.
   */
  public void setMaxHits(int maxHits) {
    this.maxHits = maxHits;
  }

  synchronized public void optimizeIndex() {
//		if(optimizeInProgress) {
//			return;
//		}
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				optimizeInProgress = true;
//				long id = Thread.currentThread().getId();
//				Thread.currentThread().setName("CRIS Optimize Thread[" + id + "]");
//				logger.info("Starting index optimization.");
//				AccessController.doPrivileged(new PrivilegedAction<Void>() {
//
//					@Override
//					public Void run() {
//						try {
//							getIndexWriter().optimize(true);
//							commit();
//							logger.info("Index optimized.");
//						} catch(IOException ex) {
//							logger.error(ex.getMessage());
//							throw new RuntimeException("Could not optimize index.", ex);
//						} catch(OutOfMemoryError ex) {
//							logger.error(ex.getMessage());
//						} finally {
//							optimizeInProgress = false;
//							closeIndexWriter();
//						}
//						return null;
//					}
//				});
//			}
//		}).start();
  }

  @Override
  public void finalize()
          throws Throwable {
    luceneEngine.finalize();
    super.finalize();

  }
  
  public void clear() {
    try {
      luceneEngine.getIndexWriter().deleteAll();
      luceneEngine.commitChanges();
    } catch (IOException ex) {
      logger.error("Could not clear the index: " + ex.getMessage());
    }
  }


  /**
   * Index a resource.
   *
   * @param resource	the resource to index.
   */
  protected void indexResource(Resource resource) {
    if (resource instanceof UriRef) {
      try {
        indexNamedResource((UriRef) resource);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      indexAnonymousResource(resource);
    }
  }

  private void indexNamedResource(UriRef uriRef)
          throws IOException {

    Term term = new Term(IndexConstants.URI_FIELD_NAME, uriRef.getUnicodeString());
    luceneEngine.getIndexWriter().deleteDocuments(term);
    luceneEngine.getIndexWriter().commit();
    //the reindexing might be caused by the removal of a type statement

    GraphNode node = new GraphNode(uriRef, this.baseGraph);
    List<UriRef> types = new ArrayList<UriRef>();
    Lock lock = node.readLock();
    lock.lock();
    try {
      Iterator<Resource> resources = node.getObjects(RDF.type);
      while (resources.hasNext()) {
        Resource res = resources.next();
        if (res instanceof UriRef) {
          types.add((UriRef) res);
        }
      }
    } finally {
      lock.unlock();
    }
    for (UriRef type : types) {
      if (type2IndexedProperties.containsKey(type)) {
        Document doc = resourceToDocument(uriRef, type);
        doc.add(new Field(IndexConstants.URI_FIELD_NAME, uriRef.getUnicodeString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        luceneEngine.getIndexWriter().addDocument(doc);
        //writer.addDocument(doc);
      }
    }
    luceneEngine.commitChanges();
  }

  private Document resourceToDocument(UriRef resource, UriRef resourceType) {
    Document doc = new Document();
    Set<VirtualProperty> indexedProperties = type2IndexedProperties.get(resourceType);
    logger.info("indexing " + resource + " considering " + indexedProperties.size() + " properties (" + indexedProperties + ")");
    for (VirtualProperty vProperty : indexedProperties) {
      logger.info("indexing " + vProperty + " with values " + (vProperty.value(
              new GraphNode(resource, this.baseGraph))).size());
      for (String propertyValue : vProperty.value(new GraphNode(resource, this.baseGraph))) {
        logger.info("indexing " + vProperty + "(" + vProperty.getStringKey() + ") with value " + (propertyValue));
        //for sorting
        doc.add(new Field(IndexConstants.SORT_PREFIX + vProperty.getStringKey(),
                propertyValue,
                Field.Store.YES,
                Field.Index.NOT_ANALYZED_NO_NORMS));
        //for searching (the extra field doesn't cost much time)
        doc.add(new Field(vProperty.getStringKey(),
                propertyValue,
                Field.Store.NO,
                Field.Index.NOT_ANALYZED));
        doc.add(new Field(vProperty.getStringKey(),
                propertyValue,
                Field.Store.NO,
                Field.Index.ANALYZED));
      }
    }
    return doc;
  }

  protected void indexAnonymousResource(Resource resource) {
    logger.warn("Currently only indexing named resources is supported");
    /*val doc = resourceToDocument(resource)
     doc.add(new Field(URI_FIELD_NAME, getIdentifier(resource), Field.Store.YES, Field.Index.ANALYZED))
     writer.addDocument(doc)*/
  }

  protected NonLiteral getResource(Document d) {
    return new UriRef(d.get(IndexConstants.URI_FIELD_NAME));
  }

  /**
   * Find resources using conditions.
   *
   * @param conditions a list of conditions to construct a query from.
   * @return a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(List<? extends Condition> conditions)
          throws ParseException {
    return findResources(conditions, new FacetCollector[0]);
  }

  /**
   * Find resources using conditions and collect facets.
   *
   * @param conditions a list of conditions to construct a query from.
   * @param facetCollectors facet collectors to apply to the query result.
   * @return a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(List<? extends Condition> conditions,
          FacetCollector... facetCollectors) throws ParseException {
    return findResources(conditions, null, facetCollectors);
  }

  /**
   * Find resources using conditions and collect facets and a sort order.
   *
   * @param conditions a list of conditions to construct a query from.
   * @param facetCollectors facet collectors to apply to the query result.
   * @param sortSpecification specifies the sort order.
   * @return a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(List<? extends Condition> conditions,
          SortSpecification sortSpecification,
          FacetCollector... facetCollectors) throws ParseException {
    return findResources(conditions, sortSpecification,
            Arrays.asList(facetCollectors), 0, maxHits + 1);
  }

  /**
   * Find resource with given property whose value matches a pattern.
   *
   * @param property	The property to which to apply the pattern.
   * @param pattern	The pattern from which to construct a query.
   * @return	a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(UriRef property, String pattern)
          throws ParseException {
    return findResources(property, pattern, false);
  }

  /**
   * Find resource with given property whose value matches a pattern.
   *
   * @param property	The property to which to apply the pattern.
   * @param pattern	The pattern from which to construct a query.
   * @param escapePattern	whether to escape reserved characters in the pattern
   * @return list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(UriRef property, String pattern, boolean escapePattern)
          throws ParseException {
    return findResources(property, pattern, escapePattern, new FacetCollector[0]);
  }

  /**
   * Find resource with given property whose value matches a pattern and collect
   * facets.
   *
   * @param property	The property to which to apply the pattern.
   * @param pattern	The pattern from which to construct a query.
   * @param escapePattern	whether to escape reserved characters in the pattern
   * @param facetCollectors facet collectors to apply to the query result.
   * @return	a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(UriRef property, String pattern,
          boolean escapePattern, FacetCollector... facetCollectors)
          throws ParseException {

    List<Condition> list = new ArrayList<Condition>();
    if (escapePattern) {
      pattern = QueryParser.escape(pattern);
    }
    list.add(new WildcardCondition(new PropertyHolder(property), pattern));
    return findResources(list, facetCollectors);
  }

  /**
   * Find resource with given property whose value matches a pattern and sort
   * order and collect facets.
   *
   * @param property	The property to which to apply the pattern.
   * @param pattern	The pattern from which to construct a query.
   * @param escapePattern	whether to escape reserved characters in the pattern
   * @param sortSpecification	specifies the sort order.
   * @param facetCollectors facet collectors to apply to the query result.
   * @return	a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(UriRef property, String pattern,
          boolean escapePattern, SortSpecification sortSpecification,
          FacetCollector... facetCollectors) throws ParseException {

    List<Condition> list = new ArrayList<Condition>();
    if (escapePattern) {
      pattern = QueryParser.escape(pattern);
    }
    list.add(new WildcardCondition(new PropertyHolder(property), pattern));
    return findResources(list, sortSpecification, facetCollectors);
  }

  /**
   * Find resource with given VirtualProperty whose value matches a pattern.
   *
   * @param property	The property to which to apply the pattern.
   * @param pattern	The pattern from which to construct a query.
   * @return	a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(VirtualProperty property, String pattern)
          throws ParseException {
    return findResources(property, pattern, false);
  }

  /**
   * Find resource with given VirtualProperty whose value matches a pattern.
   *
   * @param property	The property to which to apply the pattern.
   * @param pattern	The pattern from which to construct a query.
   * @param escapePattern	whether to escape reserved characters in the pattern
   * @return	a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(VirtualProperty property, String pattern,
          boolean escapePattern) throws ParseException {

    return findResources(property, pattern, escapePattern, new FacetCollector[0]);
  }

  /**
   * Find resource with given VirtualProperty whose value matches a pattern and
   * collect facets.
   *
   * @param property	The property to which to apply the pattern.
   * @param pattern	The pattern from which to construct a query.
   * @param escapePattern	whether to escape reserved characters in the pattern
   * @param facetCollectors	facet collectors to apply to the query result.
   * @return	a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(VirtualProperty property, String pattern,
          boolean escapePattern, FacetCollector... facetCollectors)
          throws ParseException {

    List<Condition> list = new ArrayList<Condition>();
    if (escapePattern) {
      pattern = QueryParser.escape(pattern);
    }
    list.add(new WildcardCondition(property, pattern));
    return findResources(list, facetCollectors);
  }

  /**
   * Find resource with given VirtualProperty whose value matches a pattern and
   * sort specification and collect facets.
   *
   * @param property	The property to which to apply the pattern.
   * @param pattern	The pattern from which to construct a query.
   * @param escapePattern	whether to escape reserved characters in the pattern
   * @param sortSpecification	specifies the sort order.
   * @param facetCollectors	facet collectors to apply to the query result.
   * @return	a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(VirtualProperty property, String pattern,
          boolean escapePattern, SortSpecification sortSpecification,
          FacetCollector... facetCollectors) throws ParseException {

    List<Condition> list = new ArrayList<Condition>();
    if (escapePattern) {
      pattern = QueryParser.escape(pattern);
    }
    list.add(new WildcardCondition(property, pattern));
    return findResources(list, sortSpecification, facetCollectors);
  }

  /**
   * Find resources using conditions and collect facets and specify a sort
   * order.
   *
   * This method allows to specify the indices of the query results to return
   * (e.g. for pagination).
   *
   * @param conditions a list of conditions to construct a query from.
   * @param facetCollectors Facet collectors to apply to the query result. Can
   * be {@link Collections#EMPTY_LIST}, if not used.
   * @param sortSpecification Specifies the sort order. Can be null, if not
   * used.
   * @param from return results starting from this index (inclusive).
   * @param to return results until this index (exclusive).
   * @return a list of resources that match the query.
   *
   * @throws ParseException when the resulting query is illegal.
   */
  public List<NonLiteral> findResources(List<? extends Condition> conditions,
          SortSpecification sortSpecification,
          List<FacetCollector> facetCollectors, int from, int to)
          throws ParseException {

    if (from < 0) {
      from = 0;
    }

    if (to < from) {
      to = from + 1;
    }

    if (facetCollectors == null) {
      facetCollectors = Collections.EMPTY_LIST;
    }

    BooleanQuery booleanQuery = new BooleanQuery();
    for (Condition c : conditions) {
      booleanQuery.add(c.query(), BooleanClause.Occur.MUST);
    }

    IndexSearcher searcher;
    List<NonLiteral> result = new ArrayList<NonLiteral>();

    try {
      searcher = luceneEngine.getSearcherManager().acquire();

      ScoreDoc[] hits = null;
      if (sortSpecification != null) {
        Sort sort = new Sort(sortSpecification.getSortFields());
        TopDocs topFieldDocs = searcher.search(booleanQuery, null, to, sort);
        hits = topFieldDocs.scoreDocs;
      } else {
        TopScoreDocCollector collector = TopScoreDocCollector.create(to, true);
        searcher.search(booleanQuery, collector);
        hits = collector.topDocs().scoreDocs;
      }


      for (int i = from; i < hits.length; ++i) {
        int docId = hits[i].doc;
        Document d;
        try {
          d = searcher.doc(docId);
          collectFacets(facetCollectors, d);
          result.add(getResource(d));
        } catch (IOException ex) {
          logger.error("CRIS Error: ", ex);
        }
      }

      for (FacetCollector facetCollector : facetCollectors) {
        facetCollector.postProcess();
      }

      //release the searcher
      luceneEngine.getSearcherManager().release(searcher);

    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }


    return result;
  }

  private void collectFacets(List<FacetCollector> facetCollectors, Document d) {
    if (facetCollectors.size() > 0) {
      for (FacetCollector facetCollector : facetCollectors) {
        Map<VirtualProperty, Map<String, Object>> facetMap =
                facetCollector.getFacetMap();
        for (VirtualProperty property : facetMap.keySet()) {
          String[] values = d.getValues(IndexConstants.SORT_PREFIX + property.getStringKey());
          if (values != null) {
            for (String value : values) {
              facetCollector.addFacetValue(property, value);
            }
          }
        }
      }
    }
  }
}
