/** University Library of Frankfurt. 2018
* Specialised Information Service Biodiversity Research
*/

package de.unifrankfurt.taggedtexttokenizer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The factory class for the TaggedTextTokenizer.*/
public class TaggedTextTokenizerFactory extends TokenizerFactory implements ResourceLoaderAware {
  
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String SEARCH_ATTRIBUTES_FILE = "searchAttributesFile";
  private static final String INDEX_ALL = "indexAll";
  private static final String EXCLUDE_ATTRIBUTE_FILE = "excludeAttributesFile";
  
  private final String searchedAttributesFiles;
  private final boolean indexAll;
  private final String excludeAttributesFile;
  private HashMap<String, String[]> searchedAttributes = new HashMap<String, String[]>();
  private List<String> excludedAttributes = new ArrayList<String>();
  
  /** Constructor. */
  public TaggedTextTokenizerFactory(Map<String, String> args) {
    super(args);

    this.searchedAttributesFiles = args.remove(SEARCH_ATTRIBUTES_FILE);
    this.indexAll = Boolean.parseBoolean(args.remove(INDEX_ALL));
    this.excludeAttributesFile = args.remove(EXCLUDE_ATTRIBUTE_FILE);

    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }
  
  /** Create a TaggedTextTokenizer and return it. */
  @Override
  public TaggedTextTokenizer create(AttributeFactory factory) {
    return new TaggedTextTokenizer(factory, searchedAttributes, excludedAttributes, indexAll);
  }
  
  /** Loading of external files. */
  @Override
  public void inform(ResourceLoader loader) throws IOException {
    if (searchedAttributesFiles != null) {
      try (InputStream stream = loader.openResource(searchedAttributesFiles)) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

        JsonElement element = new JsonParser().parse(new InputStreamReader(stream, decoder));
        Type type = new TypeToken<HashMap<String, String[]>>(){}.getType();
        this.searchedAttributes = new Gson().fromJson(element, type);
      } catch (IOException e) {
        this.searchedAttributes = new HashMap<String, String[]>();
      }
    }
    
    if (excludeAttributesFile != null) {
      try (InputStream stream = loader.openResource(excludeAttributesFile)) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

        String inputString = IOUtils.toString(new InputStreamReader(stream, decoder));
        this.excludedAttributes = Arrays.asList(inputString.split(","));
        
        // Remove all leading and trailing whitespaces
        for (int i = 0; i < this.excludedAttributes.size(); ++i) {
          String e = this.excludedAttributes.get(i);
          this.excludedAttributes.set(i, e.trim());
        }
      } catch (IOException e) {
        this.excludedAttributes = new ArrayList<String>();
      }
    }
  }
}
