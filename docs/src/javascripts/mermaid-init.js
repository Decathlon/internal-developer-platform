// Initialize Mermaid diagrams
// NOTE: Keep this list in sync with supported Mermaid diagram types.
// When upgrading Mermaid or enabling new diagram types, update this regex accordingly.
var mermaidKeywords = /^(flowchart|graph|sequenceDiagram|classDiagram|stateDiagram|erDiagram|gantt|pie|journey|gitGraph|mindmap|timeline|quadrantChart|requirementDiagram)\b/;

function initMermaid() {
  if (typeof mermaid === 'undefined') return;

  // Find all code blocks that contain mermaid content (flowchart, graph, sequenceDiagram, etc.)

  document.querySelectorAll('div.highlight pre code, pre code').forEach(function(codeBlock) {
    var text = codeBlock.textContent.trim();
    if (mermaidKeywords.test(text)) {
      var container = codeBlock.closest('div.highlight') || codeBlock.parentElement;
      var div = document.createElement('div');
      div.className = 'mermaid';
      div.textContent = text;
      container.parentNode.replaceChild(div, container);
    }
  });

  // Initialize mermaid
  mermaid.initialize({
    startOnLoad: false,
    theme: 'default',
    securityLevel: 'strict',
    flowchart: {
      useMaxWidth: true,
      htmlLabels: true
    }
  });

  // Run mermaid on all .mermaid elements
  mermaid.run();
}

// Run on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initMermaid);
} else {
  initMermaid();
}

// Re-initialize on page navigation (for SPA-like navigation with instant loading)
// `document$` is a navigation/document observable provided by some documentation themes
// (e.g., Material for MkDocs) that emits on client-side page changes. When present,
// subscribe so Mermaid diagrams are re-initialized after each navigation event.
if (typeof document$ !== 'undefined') {
  document$.subscribe(function() {
    initMermaid();
  });
}
