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

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides Lucene tools such as
 * <code>IndexWriter</code> and
 * <code>IndexSearcher</code>.
 *
 * @author tio
 */
public final class LuceneEngine {

  private static volatile LuceneEngine instance = null;

  public static LuceneEngine getInstance(final Directory directory, final Analyzer analyzer) throws IOException {
    if (instance == null) {
      synchronized (LuceneEngine.class) {
        // Double check
        if (instance == null) {
          instance = new LuceneEngine(directory, analyzer);
        }
      }
    }
    return instance;
  }
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Directory directory = null;
  private SearcherManager manager = null;
  private IndexWriter indexWriter = null;
  private Analyzer analyzer = null;

  // private constructor
  private LuceneEngine() {
  }

  private LuceneEngine(Analyzer analyzer) throws IOException {
    this(new RAMDirectory(), analyzer);
  }

  private LuceneEngine(Directory directory, Analyzer analyzer) throws IOException {
    this.directory = directory;
    this.analyzer = analyzer;
    this.indexWriter = new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_44, analyzer));
    this.manager = new SearcherManager(this.indexWriter, true, null);
  }

  /**
   * Return a IndexWriter
   */
  public IndexWriter getIndexWriter() throws RuntimeException {
    return getIndexWriter(false);
  }

  /**
   * Return a IndexWriter
   *
   * @param createDir specifies the path to a directory where the index should
   * be stored.
   */
  public IndexWriter getIndexWriter(boolean createDir) {
    if (indexWriter != null) {
      return indexWriter;
    }
    try {
      this.indexWriter = new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_44, analyzer));
      this.manager = new SearcherManager(this.indexWriter, true, null);
      return indexWriter;
    } catch (Exception ex) {
      logger.error(ex.getMessage());
    }
    throw new RuntimeException("Could not initialize IndexWriter");
  }

  /**
   * Returns a SearchManager
   *
   */
  public SearcherManager getSearcherManager() throws RuntimeException {
    return this.manager;
  }

  /**
   * Returns the Analyzer;
   *
   * @return analyzer;
   */
  public Analyzer getAnalyzer() {
    return this.analyzer;
  }

  /**
   * Starts index optimization. Optimization is started in a separate thread.
   * This method does not wait for optimization to finish but returns
   * immediately after starting the optimize thread.
   */
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
//							getIndexWriter().getNextMerge()..optimize(true);
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
  protected void finalize() throws Throwable {
    try {
      logger.warn("lucene search in finalized method closed");
      this.indexWriter.close(true);
      this.manager.close();
    } finally {
      super.finalize();
    }
  }

  /**
   * Commits all pending changes to the index
   *
   */
  protected void commitChanges() {
    try {
      commit();
    } catch (IOException ex) {
      logger.error("Could not commit changes: " + ex.getMessage());
    }
  }

  private void commit() throws IOException {
    try {
      getIndexWriter().commit();
    } catch (IOException ex) {
      logger.error(ex.getMessage());
    } catch (AlreadyClosedException ex) {
      logger.warn("IndexWriter already Closed: " + ex.getMessage());
      indexWriter = null;
      getIndexWriter();
      commitChanges();
    } finally {
      this.manager.maybeRefresh();
    }
  }
}
