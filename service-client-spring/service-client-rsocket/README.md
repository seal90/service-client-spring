# To be optimized
* TODOs on code
* forward metadata
* rsocket exchange like ServerWebExchange
  * before encode and after decode
  * implement RSocketInterceptor
    * [core](src/main/java/io/github/seal90/serviceclient/rsocket/core)

// MetadataExtractor
//  EntryExtractor<?> extractor = this.registrations.get(mimeType);
//	if (extractor != null) {
//		extractor.extract(content, result);
//		return;
//	}
// MetadataEncoder
//  ReactiveAdapter adapter = this.strategies.reactiveAdapterRegistry().getAdapter(metadata.getClass());