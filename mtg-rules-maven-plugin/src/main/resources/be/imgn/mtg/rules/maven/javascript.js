
        // Build a map of lowercase terms for matching
        const glossaryLower = {};
        for (const term of Object.keys(glossary)) {
            glossaryLower[term.toLowerCase()] = { term, definition: glossary[term] };
        }

        // Sort terms by length (longest first) for matching
        const sortedTerms = Object.keys(glossaryLower).sort((a, b) => b.length - a.length);

        // Theme handling
        function getPreferredTheme() {
            const stored = localStorage.getItem('theme');
            if (stored) return stored;
            return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
        }

        function setTheme(theme) {
            document.documentElement.setAttribute('data-theme', theme);
            localStorage.setItem('theme', theme);
            document.getElementById('theme-icon').textContent = theme === 'dark' ? '☀️' : '🌙';
        }

        function toggleTheme() {
            const current = document.documentElement.getAttribute('data-theme') || getPreferredTheme();
            setTheme(current === 'dark' ? 'light' : 'dark');
        }

        // Initialize theme
        setTheme(getPreferredTheme());

        // Listen for system theme changes
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
            if (!localStorage.getItem('theme')) {
                setTheme(e.matches ? 'dark' : 'light');
            }
        });

        // Back to top button
        const backToTop = document.getElementById('back-to-top');
        window.addEventListener('scroll', () => {
            if (window.scrollY > 500) {
                backToTop.classList.add('visible');
            } else {
                backToTop.classList.remove('visible');
            }
        });

        // Smooth scroll for anchor links
        document.querySelectorAll('a[href^="#"]').forEach(anchor => {
            anchor.addEventListener('click', function(e) {
                e.preventDefault();
                const target = document.querySelector(this.getAttribute('href'));
                if (target) {
                    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    history.pushState(null, null, this.getAttribute('href'));
                }
            });
        });

        // Tooltip element
        const tooltip = document.getElementById('tooltip');
        let activeGlossaryTerm = null;

        function showTooltip(element, definition) {
            tooltip.textContent = definition;
            tooltip.classList.add('visible');

            const rect = element.getBoundingClientRect();
            const tooltipRect = tooltip.getBoundingClientRect();

            let top = rect.top - tooltipRect.height - 10;
            let left = rect.left + (rect.width / 2) - (tooltipRect.width / 2);

            // Keep within viewport
            if (top < 10) {
                top = rect.bottom + 10;
            }
            if (left < 10) {
                left = 10;
            }
            if (left + tooltipRect.width > window.innerWidth - 10) {
                left = window.innerWidth - tooltipRect.width - 10;
            }

            tooltip.style.top = top + 'px';
            tooltip.style.left = left + 'px';
        }

        function hideTooltip() {
            tooltip.classList.remove('visible');
            activeGlossaryTerm = null;
        }

        // Mark glossary terms in rule text and glossary definitions
        function markGlossaryTerms() {
            const elements = document.querySelectorAll('.rule, .glossary-entry');

            elements.forEach(element => {
                // If in a glossary entry, get the term being defined so we can skip it
                let currentEntryTerm = null;
                if (element.classList.contains('glossary-entry')) {
                    const titleEl = element.querySelector('.glossary-term-title');
                    if (titleEl) {
                        currentEntryTerm = titleEl.textContent.toLowerCase();
                    }
                }

                const walker = document.createTreeWalker(
                    element,
                    NodeFilter.SHOW_TEXT,
                    null,
                    false
                );

                const textNodes = [];
                let node;
                while (node = walker.nextNode()) {
                    // Skip if parent is already a glossary term, link, or glossary term title
                    if (node.parentElement.classList.contains('glossary-term') ||
                        node.parentElement.classList.contains('glossary-term-title') ||
                        node.parentElement.classList.contains('rule-number') ||
                        node.parentElement.tagName === 'A') {
                        continue;
                    }
                    textNodes.push(node);
                }

                textNodes.forEach(textNode => {
                    const text = textNode.textContent;

                    // Find all matching glossary terms with their positions
                    const matches = [];
                    for (const termLower of sortedTerms) {
                        if (termLower.length < 4) continue;

                        // Skip if this is the term being defined in this glossary entry
                        if (currentEntryTerm && termLower === currentEntryTerm) continue;

                        const regex = new RegExp('\\b(' + termLower.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + ')\\b', 'gi');
                        let match;
                        while ((match = regex.exec(text)) !== null) {
                            // Check if this position overlaps with an existing match
                            const start = match.index;
                            const end = start + match[1].length;
                            const overlaps = matches.some(m => !(end <= m.start || start >= m.end));
                            if (!overlaps) {
                                matches.push({
                                    start,
                                    end,
                                    text: match[1],
                                    term: glossaryLower[termLower].term
                                });
                            }
                        }
                    }

                    if (matches.length === 0) return;

                    // Sort by position (descending) to replace from end to start
                    matches.sort((a, b) => b.start - a.start);

                    let result = text;
                    for (const m of matches) {
                        const termId = m.term.toLowerCase().replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '');
                        const replacement = '<a href="#glossary-' + termId + '" class="glossary-term" data-term="' + m.term + '">' + m.text + '</a>';
                        result = result.substring(0, m.start) + replacement + result.substring(m.end);
                    }

                    const span = document.createElement('span');
                    span.innerHTML = result;
                    textNode.parentNode.replaceChild(span, textNode);
                });
            });
        }

        // Add event listeners for glossary terms
        document.addEventListener('mouseover', (e) => {
            if (e.target.classList.contains('glossary-term')) {
                const term = e.target.dataset.term;
                if (term && glossary[term]) {
                    activeGlossaryTerm = e.target;
                    showTooltip(e.target, glossary[term]);
                }
            }
        });

        document.addEventListener('mouseout', (e) => {
            if (e.target.classList.contains('glossary-term')) {
                hideTooltip();
            }
        });

        // Mark glossary terms after page loads
        document.addEventListener('DOMContentLoaded', markGlossaryTerms);
