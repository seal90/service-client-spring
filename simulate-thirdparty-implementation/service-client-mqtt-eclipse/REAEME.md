
# chain
## CompositeFilter
```java
  private static class VirtualFilterChain implements FilterChain {
    private final FilterChain originalChain;
    private final List<? extends Filter> additionalFilters;
    private int currentPosition = 0;

    public VirtualFilterChain(FilterChain chain, List<? extends Filter> additionalFilters) {
      this.originalChain = chain;
      this.additionalFilters = additionalFilters;
    }

    public void doFilter(final ServletRequest request, final ServletResponse response) throws IOException, ServletException {
      if (this.currentPosition == this.additionalFilters.size()) {
        this.originalChain.doFilter(request, response);
      } else {
        ++this.currentPosition;
        Filter nextFilter = (Filter)this.additionalFilters.get(this.currentPosition - 1);
        nextFilter.doFilter(request, response, this);
      }

    }
  }
```

## HandlerExecutionChain
```java
  boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
    for(int i = 0; i < this.interceptorList.size(); this.interceptorIndex = i++) {
      HandlerInterceptor interceptor = (HandlerInterceptor)this.interceptorList.get(i);
      if (!interceptor.preHandle(request, response, this.handler)) {
        this.triggerAfterCompletion(request, response, (Exception)null);
        return false;
      }
    }

    return true;
  }
```

## DefaultWebFilterChain
```java
  private static DefaultWebFilterChain initChain(List<WebFilter> filters, WebHandler handler) {
    DefaultWebFilterChain chain = new DefaultWebFilterChain(filters, handler, (WebFilter)null, (DefaultWebFilterChain)null);

    for(ListIterator<? extends WebFilter> iterator = filters.listIterator(filters.size()); iterator.hasPrevious(); chain = new DefaultWebFilterChain(filters, handler, (WebFilter)iterator.previous(), chain)) {
    }

    return chain;
  }

    public Mono<Void> filter(ServerWebExchange exchange) {
      return Mono.defer(() -> this.currentFilter != null && this.chain != null ? this.invokeFilter(this.currentFilter, this.chain, exchange) : this.handler.handle(exchange));
    }
    
    private Mono<Void> invokeFilter(WebFilter current, DefaultWebFilterChain chain, ServerWebExchange exchange) {
      String currentName = current.getClass().getName();
      return current.filter(exchange, chain).checkpoint(currentName + " [DefaultWebFilterChain]");
    }
```