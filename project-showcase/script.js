document.addEventListener('DOMContentLoaded', () => {
  // --- 0. PAGE LOADER SCREEN ---
  const loader = document.createElement('div');
  loader.className = 'page-loader';
  loader.innerHTML = '<div class="loader-spinner"></div>';
  document.body.prepend(loader);

  setTimeout(() => {
    loader.classList.add('fade-out');
    setTimeout(() => {
      loader.remove();
    }, 500); // match fade-out transition duration (0.5s)
  }, 800);

  // --- 1. NAVBAR SCROLL EFFECT ---
  const navbar = document.querySelector('.navbar');
  
  function handleNavbarScroll() {
    if (window.scrollY > 50) {
      navbar.classList.add('scrolled');
      navbar.classList.add('navbar-scrolled'); // compatibility
    } else {
      navbar.classList.remove('scrolled');
      navbar.classList.remove('navbar-scrolled');
    }
  }
  
  window.addEventListener('scroll', handleNavbarScroll);
  handleNavbarScroll(); // Initial check

  // --- 2. ACTIVE NAV LINK TRACKING VIA INTERSECTION OBSERVER ---
  const navLinks = document.querySelectorAll('.nav-links a');
  const sections = document.querySelectorAll('.section-container, .hero-section');

  const activeObserverOptions = {
    root: null,
    threshold: 0.15, // Low threshold ensures tall sections easily trigger intersection
    rootMargin: '-120px 0px -50% 0px' // Focus intersection checks on upper-middle of viewport
  };

  const activeObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const id = entry.target.getAttribute('id');
        const targetHref = id ? `#${id}` : '#';
        
        navLinks.forEach(link => {
          if (link.getAttribute('href') === targetHref) {
            link.classList.add('active');
            link.classList.add('active-link'); // compatibility
          } else {
            link.classList.remove('active');
            link.classList.remove('active-link');
          }
        });
      }
    });
  }, activeObserverOptions);

  sections.forEach(sec => activeObserver.observe(sec));

  // --- 3. SMOOTH SCROLL WITH 80PX NAVBAR OFFSET ---
  const smoothScrollSelectors = '.nav-links a, .hero-actions a, .btn[href^="#"]';
  const scrollElements = document.querySelectorAll(smoothScrollSelectors);

  scrollElements.forEach(elem => {
    elem.addEventListener('click', (e) => {
      const href = elem.getAttribute('href');
      if (href && href.startsWith('#')) {
        e.preventDefault();
        const targetId = href.substring(1);
        const targetElement = document.getElementById(targetId);
        
        if (targetElement) {
          const elementPosition = targetElement.getBoundingClientRect().top;
          const offsetPosition = elementPosition + window.pageYOffset - 80; // 80px offset
          
          window.scrollTo({
            top: offsetPosition,
            behavior: 'smooth'
          });
        } else if (href === '#') {
          window.scrollTo({
            top: 0,
            behavior: 'smooth'
          });
        }
      }
    });
  });

  // --- 4. ACCORDION FOR INTERVIEW Q&A ---
  const accordionHeaders = document.querySelectorAll('.accordion-header');

  accordionHeaders.forEach(header => {
    header.addEventListener('click', () => {
      const item = header.parentElement;
      const body = item.querySelector('.accordion-body');
      const isActive = item.classList.contains('open');

      // Close all other open items in the same accordion container
      document.querySelectorAll('.accordion-item').forEach(otherItem => {
        if (otherItem !== item) {
          otherItem.classList.remove('open');
          otherItem.classList.remove('active');
          otherItem.querySelector('.accordion-body').style.maxHeight = null;
        }
      });

      // Toggle current active item
      if (isActive) {
        item.classList.remove('open');
        item.classList.remove('active');
        body.style.maxHeight = null;
      } else {
        item.classList.add('open');
        item.classList.add('active');
        body.style.maxHeight = body.scrollHeight + 'px';
      }
    });
  });

  // --- 5. CATEGORY FILTER FOR INTERVIEW QUESTIONS ---
  const filterButtons = document.querySelectorAll('.filter-btn');
  const accordionItems = document.querySelectorAll('.accordion-item');

  filterButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      // Toggle button active state
      filterButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      const targetCategory = btn.getAttribute('data-category');

      accordionItems.forEach(item => {
        const itemCategory = item.getAttribute('data-category');
        const body = item.querySelector('.accordion-body');
        const matches = (targetCategory === 'all' || itemCategory === targetCategory);

        // Always collapse items on filter to prevent dynamic height issues
        item.classList.remove('open');
        item.classList.remove('active');
        body.style.maxHeight = null;

        if (matches) {
          item.classList.remove('filtered-out');
          item.style.display = 'block';
          // Trigger visual transition
          setTimeout(() => {
            item.style.opacity = '1';
            item.style.transform = 'translateY(0)';
          }, 10);
        } else {
          item.classList.add('filtered-out');
          item.style.opacity = '0';
          item.style.transform = 'translateY(10px)';
          setTimeout(() => {
            if (item.classList.contains('filtered-out')) {
              item.style.display = 'none';
            }
          }, 400); // match transition duration
        }
      });
    });
  });

  // --- 6. STAT COUNTER ANIMATION ---
  function animateCounters() {
    const statElements = document.querySelectorAll('.stat-value');
    if (statElements.length < 3) return;

    const duration = 1500; // 1500ms
    const targets = [
      { el: statElements[0], start: 0, end: 2, suffix: '', format: false },
      { el: statElements[1], start: 0, end: 10000, suffix: '+', format: true },
      { el: statElements[2], start: 0, end: 5, suffix: '', format: false }
    ];

    let startTimestamp = null;

    const step = (timestamp) => {
      if (!startTimestamp) startTimestamp = timestamp;
      const elapsed = timestamp - startTimestamp;
      const progress = Math.min(elapsed / duration, 1);

      // Ease-out cubic formula
      const easeProgress = 1 - Math.pow(1 - progress, 3);

      targets.forEach(t => {
        const currentVal = Math.floor(easeProgress * t.end);
        if (t.format) {
          t.el.textContent = currentVal.toLocaleString() + t.suffix;
        } else {
          t.el.textContent = currentVal + t.suffix;
        }
      });

      if (progress < 1) {
        window.requestAnimationFrame(step);
      } else {
        // Force final target state exactly
        targets.forEach(t => {
          if (t.format) {
            t.el.textContent = t.end.toLocaleString() + t.suffix;
          } else {
            t.el.textContent = t.end + t.suffix;
          }
        });
      }
    };

    window.requestAnimationFrame(step);
  }

  // Trigger stat counters immediately on page load
  animateCounters();

  // --- 7. SCROLL REVEAL ANIMATION WITH STAGGER DELAY ---
  const revealTargets = document.querySelectorAll('.card, .stat-card, .section-header, .canvas-container, .why-not-cloud-card');
  
  revealTargets.forEach(target => {
    target.classList.add('reveal-element');
  });

  const revealObserverOptions = {
    root: null,
    threshold: 0.05,
    rootMargin: '0px 0px -40px 0px' // trigger slightly before visible
  };

  const revealObserver = new IntersectionObserver((entries, observer) => {
    const parentsMap = new Map();

    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const parent = entry.target.parentElement;
        if (!parentsMap.has(parent)) {
          parentsMap.set(parent, []);
        }
        parentsMap.get(parent).push(entry.target);
      }
    });

    parentsMap.forEach((elements) => {
      elements.forEach((el, index) => {
        // Stagger delay of 100ms per element within its container group
        el.style.transitionDelay = `${index * 100}ms`;
        el.classList.add('revealed');
        observer.unobserve(el);
      });
    });
  }, revealObserverOptions);

  revealTargets.forEach(target => revealObserver.observe(target));

  // --- 8. COPY BUTTON ON CODE BLOCKS ---
  const allInlineCodes = document.querySelectorAll('code');
  
  allInlineCodes.forEach(code => {
    // Append floating copy button if text length warrants copying (> 8 characters)
    if (code.textContent.trim().length > 8) {
      code.style.position = 'relative';
      code.style.cursor = 'pointer';

      const copyBtn = document.createElement('button');
      copyBtn.className = 'copy-btn';
      copyBtn.textContent = 'Copy';
      copyBtn.setAttribute('title', 'Copy code snippet');
      code.appendChild(copyBtn);

      copyBtn.addEventListener('click', (e) => {
        e.stopPropagation(); // prevent event propagation
        const textToCopy = code.textContent.replace('Copy', '').trim();
        
        navigator.clipboard.writeText(textToCopy).then(() => {
          copyBtn.textContent = 'Copied!';
          copyBtn.classList.add('copied');
          
          const tooltip = document.createElement('span');
          tooltip.className = 'copy-tooltip';
          tooltip.textContent = 'Copied to Clipboard!';
          code.appendChild(tooltip);

          setTimeout(() => {
            copyBtn.textContent = 'Copy';
            copyBtn.classList.remove('copied');
            tooltip.remove();
          }, 1200);
        });
      });
    }
  });

  // --- 9. HAMBURGER MENU TOGGLE ---
  const navContainer = document.querySelector('.nav-container');
  const navLinksContainer = document.querySelector('.nav-links');
  
  if (navContainer && navLinksContainer) {
    const hamburger = document.createElement('button');
    hamburger.className = 'hamburger-menu';
    hamburger.innerHTML = '<span></span><span></span><span></span>';
    navContainer.appendChild(hamburger);

    hamburger.addEventListener('click', (e) => {
      e.stopPropagation();
      hamburger.classList.toggle('open');
      navLinksContainer.classList.toggle('open');
    });

    // Close menu when clicking outside
    document.addEventListener('click', (e) => {
      if (navLinksContainer.classList.contains('open')) {
        if (!hamburger.contains(e.target) && !navLinksContainer.contains(e.target)) {
          hamburger.classList.remove('open');
          navLinksContainer.classList.remove('open');
        }
      }
    });

    // Close menu when clicking any nav link
    const links = navLinksContainer.querySelectorAll('a');
    links.forEach(link => {
      link.addEventListener('click', () => {
        hamburger.classList.remove('open');
        navLinksContainer.classList.remove('open');
      });
    });

    // Close menu when resizing to desktop width
    window.addEventListener('resize', () => {
      if (window.innerWidth > 768) {
        hamburger.classList.remove('open');
        navLinksContainer.classList.remove('open');
      }
    });
  }

  // --- 10. ARCHITECTURE DIAGRAM HTML TOOLTIP ---
  const archDiagram = document.getElementById('arch-diagram');
  if (archDiagram) {
    const canvasContainer = archDiagram.parentElement;
    
    // Dynamic creation of HTML tooltip overlay
    const tooltip = document.createElement('div');
    tooltip.className = 'arch-tooltip';
    tooltip.innerHTML = `
      <div class="arch-tooltip-title"></div>
      <div class="arch-tooltip-body"></div>
    `;
    canvasContainer.appendChild(tooltip);

    // Bounding limits and metadata for components from architecture.js
    const components = [
      {
        id: "react-ui",
        name: "React UI",
        desc: "Client dashboard hosting SQL analysis visualizers, execution-plan visual breakdown, API sandbox, and live telemetry audit logs.",
        x: 500,
        y: 90,
        w: 220,
        h: 100
      },
      {
        id: "dev-toolkit",
        name: "Dev Toolkit",
        desc: "Core developer platform gateway executing SQL checks, Postgres explain-plans, secure sandboxed REST calls, and request intercept tracing.",
        x: 280,
        y: 250,
        w: 220,
        h: 100
      },
      {
        id: "rate-limiter",
        name: "Rate Limiter",
        desc: "High-throughput token bucket and sliding window rate limiter assessing transaction limits on endpoints dynamically.",
        x: 720,
        y: 250,
        w: 220,
        h: 100
      },
      {
        id: "postgres",
        name: "PostgreSQL",
        desc: "Seeded relational database storing transaction records, execution history, and correlation logging metadata.",
        x: 280,
        y: 410,
        w: 220,
        h: 100
      },
      {
        id: "redis",
        name: "Redis Cache",
        desc: "Ultra-fast distributed cache holding moving request window limits and active sliding rate buckets securely.",
        x: 720,
        y: 410,
        w: 220,
        h: 100
      }
    ];

    archDiagram.addEventListener('mousemove', (e) => {
      const rect = archDiagram.getBoundingClientRect();
      const scaleX = archDiagram.width / rect.width;
      const scaleY = archDiagram.height / rect.height;
      
      const mouseX = (e.clientX - rect.left) * scaleX;
      const mouseY = (e.clientY - rect.top) * scaleY;
      
      let hovered = null;
      for (const comp of components) {
        const rx = comp.x - comp.w / 2;
        const ry = comp.y - comp.h / 2;
        if (mouseX >= rx && mouseX <= rx + comp.w && mouseY >= ry && mouseY <= ry + comp.h) {
          hovered = comp;
          break;
        }
      }

      if (hovered) {
        tooltip.querySelector('.arch-tooltip-title').textContent = hovered.name + " Modules & Role";
        tooltip.querySelector('.arch-tooltip-body').textContent = hovered.desc;
        
        // Calculate coordinates mapping for absolute container inside relative canvas wrapper
        // Use archDiagram's clientWidth and clientHeight to support scaling and scrollable mobile layouts
        const diagramWidth = archDiagram.clientWidth;
        const diagramHeight = archDiagram.clientHeight;
        const scale = diagramWidth / archDiagram.width;
        
        const compCenterX = hovered.x * scale;
        const compTopY = (hovered.y - hovered.h / 2) * scale;
        const compBottomY = (hovered.y + hovered.h / 2) * scale;
        
        const tooltipWidth = tooltip.offsetWidth || 280;
        const tooltipHeight = tooltip.offsetHeight || 120;
        
        // Horizontal Positioning with Clamping to prevent left/right overflow
        let leftPx = compCenterX - tooltipWidth / 2;
        if (leftPx < 10) leftPx = 10;
        if (leftPx + tooltipWidth > diagramWidth - 10) {
          leftPx = diagramWidth - tooltipWidth - 10;
        }
        
        // Vertical Positioning (Default: above component)
        let topPx = compTopY - tooltipHeight - 12;
        let isFlipped = false;
        
        if (topPx < 10) {
          // Flip to be below component if it would go off the top edge
          topPx = compBottomY + 12;
          isFlipped = true;
        }
        
        // Calculate pointer triangle horizontal alignment relative to tooltip bounding box
        const pointerLeft = compCenterX - leftPx;
        const pointerLeftPct = (pointerLeft / tooltipWidth) * 100;
        const clampedPointerPct = Math.max(10, Math.min(90, pointerLeftPct));
        
        // Apply inline styles
        tooltip.style.left = `${leftPx}px`;
        tooltip.style.top = `${topPx}px`;
        tooltip.style.bottom = 'auto';
        tooltip.style.setProperty('--pointer-left', `${clampedPointerPct}%`);
        
        if (isFlipped) {
          tooltip.classList.add('flipped');
        } else {
          tooltip.classList.remove('flipped');
        }
        
        tooltip.classList.add('visible');
      } else {
        tooltip.classList.remove('visible');
      }
    });

    archDiagram.addEventListener('mouseleave', () => {
      tooltip.classList.remove('visible');
    });
  }

  // --- 11. BUILD USER GUIDE ---
  buildUserGuide();
});

// --- 12. STEP-BY-STEP USER GUIDE GENERATION ---
function buildUserGuide() {
  const guideContent = document.getElementById('guide-content');
  if (!guideContent) return;

  // 1. Build Tab Navigation HTML
  const tabsHtml = `
    <div class="guide-tabs">
      <button class="guide-tab active" data-tab="start">🚀 Getting Started</button>
      <button class="guide-tab" data-tab="sql">🔍 SQL Analyser</button>
      <button class="guide-tab" data-tab="sandbox">🌐 Endpoint Sandbox</button>
      <button class="guide-tab" data-tab="explain">📊 Execution Plan</button>
      <button class="guide-tab" data-tab="audit">📋 Audit Logs</button>
      <button class="guide-tab" data-tab="ratelimit">⚡ Rate Limiter</button>
    </div>
  `;

  // 2. Build Getting Started Panel HTML
  const startPanelHtml = `
    <div class="guide-panel active" data-panel="start">
      <!-- Progress Tracker -->
      <div class="guide-progress">
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Install</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Clone</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Configure</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Start</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Verify</span>
        </div>
      </div>

      <!-- Step List -->
      <div class="step-list">
        <!-- Step 1 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">1</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">💻</span> Check Prerequisites</h4>
            <p class="step-desc">Make sure you have these installed before starting. Each is required.</p>
            <div class="step-two-col">
              <div class="step-card-sm">
                <h5>Required</h5>
                <p>Java 17+, Maven 3.8+, Docker Desktop, Node.js 18+</p>
              </div>
              <div class="step-card-sm">
                <h5>Recommended</h5>
                <p>IntelliJ IDEA, Postman, TablePlus or DBeaver, Redis Insight</p>
              </div>
            </div>
            <div class="step-note tip">💡 Run these commands to verify your setup</div>
            <div class="step-code">java -version       # should show 17+
mvn -version        # should show 3.8+
docker --version    # should show 20+
node --version      # should show 18+</div>
          </div>
        </div>

        <!-- Step 2 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">2</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">📥</span> Clone and Open the Project</h4>
            <p class="step-desc">Clone the repository and open it in your IDE.</p>
            <div class="step-code">git clone https://github.com/CoderAnshulSoni/Developer-Utility-Platform-with-Distributed-Rate-Limiter.git
cd developer-platform</div>
            <div class="step-note info">ℹ️ The repo has three folders: rate-limiter-service, dev-toolkit-service, frontend</div>
          </div>
        </div>

        <!-- Step 3 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">3</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🐳</span> One-Command Docker Start</h4>
            <p class="step-desc">The easiest way to run everything. Docker Compose boots all 5 services in the correct order automatically.</p>
            <div class="step-code">docker-compose up --build</div>
            <div class="step-result">All 5 containers running: redis, postgres, rate-limiter, dev-toolkit, frontend</div>
            <div class="step-note warning">⚠️ First run downloads images and builds JARs — takes 3-5 minutes. Subsequent starts take under 30 seconds.</div>
          </div>
        </div>

        <!-- Step 4 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">4</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">⚙️</span> Start Services Manually (Without Docker)</h4>
            <p class="step-desc">Use this if you want to run services individually for development.</p>
            <div class="step-code"># Terminal 1 — Start Redis
docker run -p 6379:6379 redis:7-alpine

# Terminal 2 — Start PostgreSQL  
docker run -p 5432:5432 \\
  -e POSTGRES_DB=devtoolkit \\
  -e POSTGRES_PASSWORD=postgres \\
  postgres:15-alpine

# Terminal 3 — Start Rate Limiter
cd rate-limiter-service
mvn spring-boot:run

# Terminal 4 — Start Dev Toolkit
cd dev-toolkit-service
mvn spring-boot:run

# Terminal 5 — Start Frontend
cd frontend && npm install && npm start</div>
          </div>
        </div>

        <!-- Step 5 -->
        <div class="step-item">
          <div class="step-number">5</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">✅</span> Verify All Services Are Up</h4>
            <p class="step-desc">Open these URLs in your browser to confirm each service started correctly.</p>
            <div class="step-code">Frontend:      http://localhost:3000
Dev Toolkit:   http://localhost:8080/actuator/health
Rate Limiter:  http://localhost:8081/actuator/health

Expected response from health endpoints:
{"status":"UP"}</div>
            <div class="step-note tip">💡 If a service fails to start, check docker-compose logs service-name for errors</div>
          </div>
        </div>
      </div>
    </div>
  `;

  // 3. Build placeholder panels for hidden tabs
  const sqlPanelHtml = `
    <div class="guide-panel" data-panel="sql">
      <!-- Progress Tracker -->
      <div class="guide-progress">
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Open Tool</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Enter SQL</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Read Score</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Fix Issues</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Re-analyse</span>
        </div>
      </div>

      <!-- Step List -->
      <div class="step-list">
        <!-- Step 1 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">1</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🔍</span> Open the SQL Analyser</h4>
            <p class="step-desc">Click the SQL Analyser tab in the top navigation of the platform at <a href="http://localhost:3000" target="_blank" style="color: var(--accent); text-decoration: underline;">http://localhost:3000</a></p>
            <div class="step-result">You see a large text area and an Analyse button</div>
          </div>
        </div>

        <!-- Step 2 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">2</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">✏️</span> Enter Your SQL Query</h4>
            <p class="step-desc">Paste any SQL query into the text area. Try this example first to see all warning types:</p>
            <div class="step-code">SELECT * FROM orders 
WHERE customer_id IN (
  SELECT id FROM customers
)</div>
            <div class="step-note info">ℹ️ The analyser works on any SQL — SELECT, INSERT, UPDATE, DELETE. It does static analysis, no database needed.</div>
          </div>
        </div>

        <!-- Step 3 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">3</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">📊</span> Understanding the Score</h4>
            <p class="step-desc">After clicking Analyse you get a score from 0 to 100.</p>
            <div class="step-two-col">
              <div class="step-card-sm">
                <h5>🟢 Score Colours</h5>
                <p>🟢 70-100: Good query<br>🟡 40-69: Has issues, review warnings<br>🔴 0-39: Critical issues, fix before production</p>
              </div>
              <div class="step-card-sm">
                <h5>📉 Score Deductions</h5>
                <p><strong>HIGH</strong> severity warning: -20 points<br><strong>MEDIUM</strong> severity warning: -10 points<br><strong>LOW</strong> severity warning: -5 points</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 4 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">4</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">⚠️</span> Understanding Warning Types</h4>
            <p class="step-desc">Each warning tells you what is wrong and how to fix it.</p>
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; margin: 16px 0;">
              <!-- Warning 1 -->
              <div class="step-card-sm" style="display: flex; flex-direction: column; gap: 8px; justify-content: space-between;">
                <div>
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <span style="font-family: monospace; font-weight: 700; color: #7dd3fc;">SELECT_STAR</span>
                    <span class="tech-badge" style="background: rgba(239, 68, 68, 0.15); color: #ef4444; font-size: 11px; padding: 2px 8px;">HIGH</span>
                  </div>
                  <p style="color: var(--text-light); font-size: 13px; margin-bottom: 6px;">You used <code>SELECT *</code></p>
                  <p style="color: var(--text-muted); font-size: 13px; margin-bottom: 8px;"><strong>Fix:</strong> List explicit column names</p>
                </div>
                <div class="step-code" style="margin: 0; padding: 8px 12px; font-size: 12px;">SELECT id, name, email FROM users</div>
              </div>

              <!-- Warning 2 -->
              <div class="step-card-sm" style="display: flex; flex-direction: column; gap: 8px; justify-content: space-between;">
                <div>
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <span style="font-family: monospace; font-weight: 700; color: #7dd3fc;">MISSING_WHERE</span>
                    <span class="tech-badge" style="background: rgba(239, 68, 68, 0.15); color: #ef4444; font-size: 11px; padding: 2px 8px;">HIGH</span>
                  </div>
                  <p style="color: var(--text-light); font-size: 13px; margin-bottom: 6px;"><code>UPDATE</code> or <code>DELETE</code> has no <code>WHERE</code> clause</p>
                  <p style="color: var(--text-muted); font-size: 13px; margin-bottom: 8px;"><strong>Fix:</strong> Always add a <code>WHERE</code> clause</p>
                </div>
                <div class="step-code" style="margin: 0; padding: 8px 12px; font-size: 12px;">DELETE FROM sessions WHERE expired = true</div>
              </div>

              <!-- Warning 3 -->
              <div class="step-card-sm" style="display: flex; flex-direction: column; gap: 8px; justify-content: space-between;">
                <div>
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <span style="font-family: monospace; font-weight: 700; color: #7dd3fc;">NO_LIMIT</span>
                    <span class="tech-badge" style="background: rgba(245, 158, 11, 0.15); color: #f59e0b; font-size: 11px; padding: 2px 8px;">MEDIUM</span>
                  </div>
                  <p style="color: var(--text-light); font-size: 13px; margin-bottom: 6px;"><code>SELECT</code> query with no <code>LIMIT</code> clause</p>
                  <p style="color: var(--text-muted); font-size: 13px; margin-bottom: 8px;"><strong>Fix:</strong> Add <code>LIMIT</code> to prevent huge result sets</p>
                </div>
                <div class="step-code" style="margin: 0; padding: 8px 12px; font-size: 12px;">SELECT id FROM logs LIMIT 100</div>
              </div>

              <!-- Warning 4 -->
              <div class="step-card-sm" style="display: flex; flex-direction: column; gap: 8px; justify-content: space-between;">
                <div>
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <span style="font-family: monospace; font-weight: 700; color: #7dd3fc;">MULTIPLE_JOINS</span>
                    <span class="tech-badge" style="background: rgba(245, 158, 11, 0.15); color: #f59e0b; font-size: 11px; padding: 2px 8px;">MEDIUM</span>
                  </div>
                  <p style="color: var(--text-light); font-size: 13px; margin-bottom: 6px;">Query contains more than 3 <code>JOIN</code> operations</p>
                  <p style="color: var(--text-muted); font-size: 13px; margin-bottom: 8px;"><strong>Fix:</strong> Break query into CTEs or smaller queries</p>
                </div>
              </div>

              <!-- Warning 5 -->
              <div class="step-card-sm" style="display: flex; flex-direction: column; gap: 8px; justify-content: space-between;">
                <div>
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <span style="font-family: monospace; font-weight: 700; color: #7dd3fc;">CARTESIAN_PRODUCT</span>
                    <span class="tech-badge" style="background: rgba(239, 68, 68, 0.15); color: #ef4444; font-size: 11px; padding: 2px 8px;">HIGH</span>
                  </div>
                  <p style="color: var(--text-light); font-size: 13px; margin-bottom: 6px;">Query has a <code>JOIN</code> without a corresponding <code>ON</code> clause</p>
                  <p style="color: var(--text-muted); font-size: 13px; margin-bottom: 8px;"><strong>Fix:</strong> Add <code>ON</code> condition between tables</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 5 -->
        <div class="step-item">
          <div class="step-number">5</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🔄</span> Fix the Query and Re-analyse</h4>
            <p class="step-desc">Edit your query based on the suggestions and click Analyse again.</p>
            <div class="step-code">-- Before (score: 60)
SELECT * FROM orders

-- After (score: 100)
SELECT id, customer_id, total, status 
FROM orders 
WHERE status = 'pending' 
LIMIT 50</div>
            <div class="step-result">Score jumps to 100 with no warnings</div>
            <div class="step-note tip">💡 Copy the improved query directly from the analyser into your codebase</div>
          </div>
        </div>
      </div>
    </div>
  `;

  // 3. Build placeholder panels for hidden tabs
  const sandboxPanelHtml = `
    <div class="guide-panel" data-panel="sandbox">
      <!-- Progress Tracker -->
      <div class="guide-progress">
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Choose Method</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Enter URL</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Add Headers</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Send</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Read Response</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Check History</span>
        </div>
      </div>

      <!-- Step List -->
      <div class="step-list">
        <!-- Step 1 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">1</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🔀</span> Select Your HTTP Method</h4>
            <p class="step-desc">Open the Endpoint Sandbox tab. The method dropdown supports all standard verbs.</p>
            <div class="step-two-col">
              <div class="step-card-sm">
                <h5>📖 Read Operations</h5>
                <p><strong>GET:</strong> Fetch data, no body needed<br><strong>HEAD:</strong> Check headers only</p>
              </div>
              <div class="step-card-sm">
                <h5>✍️ Write Operations</h5>
                <p><strong>POST:</strong> Create new resource with body<br><strong>PUT:</strong> Replace existing resource<br><strong>PATCH:</strong> Partially update resource<br><strong>DELETE:</strong> Remove a resource</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">2</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🔗</span> Enter the Endpoint URL</h4>
            <p class="step-desc">Type the full URL you want to test. You can test any publicly accessible URL or your own local services.</p>
            <div class="step-code"># Test your own Dev Toolkit SQL endpoint
http://localhost:8080/api/sql/analyse

# Test a public API
https://jsonplaceholder.typicode.com/posts/1

# Test with path variables
https://jsonplaceholder.typicode.com/users/3</div>
            <div class="step-note warning">⚠️ Do not test URLs that require OAuth or cookie-based auth — use API key headers instead</div>
          </div>
        </div>

        <!-- Step 3 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">3</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">📝</span> Configure Request Headers</h4>
            <p class="step-desc">Enter headers as JSON in the headers field.</p>
            <div class="step-code">{
  "Content-Type": "application/json",
  "X-Client-Id": "my-test-client",
  "Authorization": "Bearer your-token-here"
}</div>
            <div class="step-note info">ℹ️ X-Client-Id is used by the Rate Limiter to track your request count. Use a unique value to avoid hitting limits during testing.</div>
          </div>
        </div>

        <!-- Step 4 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">4</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">📦</span> Enter Request Body (POST/PUT/PATCH)</h4>
            <p class="step-desc">For write operations enter the body as JSON.</p>
            <div class="step-code">{
  "sql": "SELECT * FROM users WHERE active = true"
}</div>
            <div class="step-note tip">💡 Leave body empty for GET requests. The Sandbox automatically handles this.</div>
          </div>
        </div>

        <!-- Step 5 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">5</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">📨</span> Send the Request and Read Results</h4>
            <p class="step-desc">Click Send Request and read the response panel.</p>
            <div class="step-two-col">
              <div class="step-card-sm">
                <h5>📥 Response Panel</h5>
                <p>• Status code with colour coding<br>• Full response body formatted as JSON<br>• All response headers<br>• Exact latency in milliseconds</p>
              </div>
              <div class="step-card-sm">
                <h5>🚦 Status Code Colours</h5>
                <p>🟢 2xx: Success<br>🟡 3xx: Redirect<br>🔴 4xx: Client error<br>🔴 5xx: Server error</p>
              </div>
            </div>
            <div class="step-result">Response appears within milliseconds for local services</div>
          </div>
        </div>

        <!-- Step 6 -->
        <div class="step-item">
          <div class="step-number">6</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🕒</span> View Request History</h4>
            <p class="step-desc">Scroll down to see the last 50 requests stored in PostgreSQL.</p>
            <div class="step-two-col">
              <div class="step-card-sm">
                <h5>📋 History Table</h5>
                <p>• HTTP method badge<br>• Target URL<br>• Response status code<br>• Latency in ms<br>• Timestamp</p>
              </div>
              <div class="step-card-sm">
                <h5>🛠️ Using History</h5>
                <p>• Click any row to reload that request<br>• Compare latency across multiple runs<br>• Debug intermittent failures over time</p>
              </div>
            </div>
            <div class="step-note tip">💡 History persists across page refreshes because it's stored in PostgreSQL, not browser memory</div>
          </div>
        </div>
      </div>
    </div>
  `;
  const explainPanelHtml = `
    <div class="guide-panel" data-panel="explain">
      <!-- Progress Tracker -->
      <div class="guide-progress">
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Connect DB</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Enter Query</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Run Explain</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Read Tree</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Fix Bottlenecks</span>
        </div>
      </div>

      <!-- Step List -->
      <div class="step-list">
        <!-- Step 1 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">1</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">📖</span> Understanding Execution Plans</h4>
            <p class="step-desc">An execution plan shows how PostgreSQL executes your query internally — which indexes it uses, how many rows it scans, and where the cost is highest.</p>
            <div class="step-two-col">
              <div class="step-card-sm">
                <h5>📊 What it tells you</h5>
                <p>• Which nodes are most expensive<br>• Whether indexes are being used<br>• Estimated vs actual row counts<br>• Total query cost score</p>
              </div>
              <div class="step-card-sm">
                <h5>🌿 Common node types</h5>
                <p>• <strong>Seq Scan:</strong> Full table scan — usually bad<br>• <strong>Index Scan:</strong> Using an index — good<br>• <strong>Hash Join:</strong> Joining via hash table<br>• <strong>Nested Loop:</strong> Row by row join — watch cost</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">2</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🔌</span> Verify PostgreSQL Connection</h4>
            <p class="step-desc">The Execution Plan module runs <code>EXPLAIN ANALYZE</code> against your connected PostgreSQL database.</p>
            <div class="step-code"># Verify connection via health endpoint
curl http://localhost:8080/actuator/health

# Should return:
{"status":"UP","components":{"db":{"status":"UP"}}}</div>
            <div class="step-note warning">⚠️ EXPLAIN ANALYZE actually executes the query. Do not run destructive queries (DELETE, UPDATE, DROP) through this tool — use SELECT statements only.</div>
          </div>
        </div>

        <!-- Step 3 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">3</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">✏️</span> Enter a SELECT Query to Analyse</h4>
            <p class="step-desc">Paste a SELECT query to visualise. Try this example which will show a Seq Scan:</p>
            <div class="step-code">SELECT u.id, u.name, h.request_url
FROM request_history h
JOIN audit_log u ON h.id = u.id
WHERE h.latency_ms > 100
ORDER BY h.created_at DESC</div>
            <div class="step-note info">ℹ️ Start with queries from your application that feel slow — this tool will show you exactly why</div>
          </div>
        </div>

        <!-- Step 4 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">4</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🌳</span> Reading the Visualised Plan Tree</h4>
            <p class="step-desc">The output shows each node in the execution tree.</p>
            <div class="step-two-col">
              <div class="step-card-sm">
                <h5>🎨 Node colours</h5>
                <p>🔴 <strong>Red border:</strong> Bottleneck (cost > 1000)<br>🟡 <strong>Yellow:</strong> High cost node (cost > 500)<br>🟢 <strong>Green:</strong> Efficient node</p>
              </div>
              <div class="step-card-sm">
                <h5>📋 Node details show</h5>
                <p>• Node type (Seq Scan / Index Scan etc)<br>• Estimated cost value<br>• Estimated row count<br>• Actual rows (after execution)</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 5 -->
        <div class="step-item">
          <div class="step-number">5</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🔧</span> Fix the Bottlenecks</h4>
            <p class="step-desc">Bottleneck nodes are highlighted in red. Here are the most common fixes:</p>
            <div class="step-code">-- Problem: Seq Scan on large table
-- Fix: Add an index on the filtered column
CREATE INDEX idx_request_latency 
ON request_history(latency_ms);

-- Problem: Seq Scan on JOIN column  
-- Fix: Index the foreign key
CREATE INDEX idx_audit_request_id 
ON audit_log(id);</div>
            <div class="step-result">Re-run the query after adding indexes — Seq Scan nodes should become Index Scan nodes and cost drops significantly</div>
            <div class="step-note tip">💡 Run EXPLAIN ANALYZE before and after adding an index to measure the exact improvement</div>
          </div>
        </div>
      </div>
    </div>
  `;
  const auditPanelHtml = `
    <div class="guide-panel" data-panel="audit">
      <!-- Progress Tracker -->
      <div class="guide-progress">
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Make Requests</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Open Logs</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Search</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Trace Request</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Clear</span>
        </div>
      </div>

      <!-- Step List -->
      <div class="step-list">
        <!-- Step 1 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">1</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🔄</span> Automatic Logging — No Setup Needed</h4>
            <p class="step-desc">Every API request to the Dev Toolkit is automatically logged by Spring AOP. You do not need to configure anything.</p>
            <div class="step-two-col">
              <div class="step-card-sm">
                <h5>📝 What gets logged automatically</h5>
                <p>• Unique correlation ID (UUID)<br>• API endpoint and HTTP method<br>• Response status code<br>• Request latency in milliseconds<br>• Rate limit remaining count<br>• Rate limit status (ALLOWED/BLOCKED/LIMITER_DOWN)</p>
              </div>
              <div class="step-card-sm">
                <h5>🚫 What does NOT get logged</h5>
                <p>• Request passwords or secrets<br>• Full request bodies (by design)<br>• Static file requests<br>• Actuator health check calls</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">2</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🧪</span> Generate Log Entries</h4>
            <p class="step-desc">Use other modules first to generate audit entries, then come back here.</p>
            <div class="step-code"># Or generate via curl directly
curl -X POST http://localhost:8080/api/sql/analyse \\
  -H "Content-Type: application/json" \\
  -H "X-Client-Id: test-user-1" \\
  -d '{"sql":"SELECT * FROM users"}'

curl http://localhost:8080/api/sandbox/history \\
  -H "X-Client-Id: test-user-1"</div>
            <div class="step-note info">ℹ️ Make at least 5 requests across different endpoints before exploring the logs</div>
          </div>
        </div>

        <!-- Step 3 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">3</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">📋</span> Open the Audit Logs View</h4>
            <p class="step-desc">Click the Audit Logs tab in the platform. The table loads the last 100 entries automatically.</p>
            <div class="step-two-col">
              <div class="step-card-sm">
                <h5>📋 Table columns</h5>
                <p>• Correlation ID (truncated, click to copy)<br>• Endpoint name<br>• Method badge<br>• Status code<br>• Latency (ms)<br>• Rate Limit Remaining<br>• Rate Limit Status<br>• Timestamp</p>
              </div>
              <div class="step-card-sm">
                <h5>🛡️ Status badges</h5>
                <p>• <strong>ALLOWED:</strong> Rate limiter approved request<br>• <strong>BLOCKED:</strong> Request was rate limited<br>• <strong>LIMITER_DOWN:</strong> Rate limiter was unreachable</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 4 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">4</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🔍</span> Trace a Specific Request</h4>
            <p class="step-desc">Paste a correlation ID in the search box to find a specific request.</p>
            <div class="step-code"># The correlation ID is returned in 
# the response header of every API call

curl -v http://localhost:8080/api/sandbox/history \\
  -H "X-Client-Id: test-user-1" 2>&1 \\
  | grep X-Correlation-Id

# Copy that ID and paste it in the search box</div>
            <div class="step-result">The table filters to show only that specific request entry</div>
            <div class="step-note tip">💡 In a microservices system, pass the same correlation ID to downstream services so you can trace a request across all services in one search</div>
          </div>
        </div>

        <!-- Step 5 -->
        <div class="step-item">
          <div class="step-number">5</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🗑️</span> Clear Audit Logs</h4>
            <p class="step-desc">Use the Clear button to delete all audit entries. Useful when testing.</p>
            <div class="step-note warning">⚠️ Clearing logs is permanent and cannot be undone. In production you would archive logs before deleting them.</div>
          </div>
        </div>
      </div>
    </div>
  `;
  const rateLimitPanelHtml = `
    <div class="guide-panel" data-panel="ratelimit">
      <!-- Progress Tracker -->
      <div class="guide-progress">
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">View Policies</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Simulate Traffic</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Watch 429</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Reset</span>
        </div>
        <div class="progress-line done"></div>
        <div class="progress-step">
          <div class="progress-dot done"></div>
          <span class="progress-label done">Change Algorithm</span>
        </div>
      </div>

      <!-- Step List -->
      <div class="step-list">
        <!-- Step 1 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">1</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">📋</span> See the Pre-configured Rate Limit Policies</h4>
            <p class="step-desc">On startup the Rate Limiter seeds four default policies.</p>
            <div class="step-code"># View all policies via API
curl http://localhost:8081/api/policies

# Expected response:
[
  {
    "endpoint": "/api/sql/analyse",
    "algorithm": "SLIDING_WINDOW",
    "maxRequests": 20,
    "windowSeconds": 60
  },
  {
    "endpoint": "/api/sql/explain",
    "algorithm": "TOKEN_BUCKET", 
    "maxRequests": 5,
    "windowSeconds": 60
  },
  {
    "endpoint": "/api/sandbox/request",
    "algorithm": "SLIDING_WINDOW",
    "maxRequests": 10,
    "windowSeconds": 60
  },
  {
    "endpoint": "/api/audit/logs",
    "algorithm": "SLIDING_WINDOW",
    "maxRequests": 30,
    "windowSeconds": 60
  }
]</div>
          </div>
        </div>

        <!-- Step 2 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">2</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">📊</span> View Live Status in the Dashboard</h4>
            <p class="step-desc">Click Rate Monitor tab in the platform at http://localhost:3000</p>
            <div class="step-two-col">
              <div class="step-card-sm">
                <h5>📊 Status cards show</h5>
                <p>• Current remaining requests<br>• Total limit for the window<br>• Live countdown to reset<br>• Color coded progress bar</p>
              </div>
              <div class="step-card-sm">
                <h5>🚦 Colours mean</h5>
                <p>🟢 <strong>Green:</strong> More than 50% remaining<br>🟡 <strong>Yellow:</strong> 20-50% remaining<br>🔴 <strong>Red:</strong> Under 20% remaining</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 3 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">3</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🔥</span> Trigger a Rate Limit (429 Response)</h4>
            <p class="step-desc">Use the Client Simulator to fire rapid requests and watch the limit hit.</p>
            <div class="step-code"># Fire requests rapidly via curl loop
for i in {1..25}; do
  curl -s -o /dev/null -w "%{http_code}\\n" \\
    -X POST http://localhost:8080/api/sql/analyse \\
    -H "Content-Type: application/json" \\
    -H "X-Client-Id: stress-test-client" \\
    -d '{"sql":"SELECT id FROM users LIMIT 1"}'
done</div>
            <div class="step-result">First 20 requests return 200. Requests 21 onwards return 429 with Retry-After header showing seconds to wait</div>
            <div class="step-note info">ℹ️ Watch the progress bar in the dashboard drain from green to red as you fire requests</div>
          </div>
        </div>

        <!-- Step 4 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">4</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🛑</span> Reading the 429 Response</h4>
            <p class="step-desc">When rate limited, the response includes details about when to retry.</p>
            <div class="step-code">HTTP/1.1 429 Too Many Requests
Retry-After: 43
Content-Type: application/json

{
  "error": "Rate limit exceeded",
  "endpoint": "/api/sql/analyse",
  "retryAfter": 43,
  "limit": "20 requests/minute"
}</div>
            <div class="step-note tip">💡 Well-behaved API clients read the Retry-After header and wait that many seconds before retrying — implement this in any client that calls rate-limited APIs</div>
          </div>
        </div>

        <!-- Step 5 -->
        <div class="step-item">
          <div class="step-line"></div>
          <div class="step-number">5</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">➕</span> Add or Update a Rate Limit Policy</h4>
            <p class="step-desc">You can add policies for any endpoint dynamically without restart.</p>
            <div class="step-code"># Create a strict policy for a new endpoint
curl -X POST http://localhost:8081/api/policies \\
  -H "Content-Type: application/json" \\
  -d '{
    "endpoint": "/api/custom/heavy",
    "algorithm": "TOKEN_BUCKET",
    "maxRequests": 3,
    "windowSeconds": 60
  }'

# Update an existing policy
curl -X PUT \\
  http://localhost:8081/api/policies/api/sql/analyse \\
  -H "Content-Type: application/json" \\
  -d '{
    "maxRequests": 50,
    "windowSeconds": 60
  }'</div>
            <div class="step-result">New policy is active immediately. No restart required.</div>
          </div>
        </div>

        <!-- Step 6 -->
        <div class="step-item">
          <div class="step-number">6</div>
          <div class="step-content">
            <h4 class="step-title"><span class="step-icon">🔄</span> Reset Rate Limit for a Specific Client</h4>
            <p class="step-desc">Use the admin panel or API to reset counters for a specific client.</p>
            <div class="step-code"># Reset via Admin API
curl -X DELETE \\
  http://localhost:8080/admin/rate-limits/stress-test-client

# Or use the Admin Panel in the Rate Monitor tab
# Find the client in the table and click Reset</div>
            <div class="step-result">Client's counter resets to 0 immediately. They can make requests again right away.</div>
            <div class="step-note warning">⚠️ Only reset in testing or when a legitimate client was incorrectly blocked. In production, log all manual resets for audit.</div>
          </div>
        </div>
      </div>
    </div>
  `;

  // Inject content into guide-content wrapper
  guideContent.innerHTML = tabsHtml + startPanelHtml + sqlPanelHtml + sandboxPanelHtml + explainPanelHtml + auditPanelHtml + rateLimitPanelHtml;

  // Connect Tab click and Copy button behaviors
  initGuideTabs();
}

function initGuideTabs() {
  // 1. Select all elements with class "guide-tab"
  const tabs = document.querySelectorAll('.guide-tab');
  
  // 2. Select all elements with class "guide-panel"
  const panels = document.querySelectorAll('.guide-panel');

  // 3. On click of any guide-tab:
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      // a. Remove "active" from all guide-tabs
      tabs.forEach(t => t.classList.remove('active'));
      
      // b. Remove "active" from all guide-panels
      panels.forEach(p => p.classList.remove('active'));
      
      // c. Add "active" to clicked tab
      tab.classList.add('active');
      
      // d. Find panel where data-tab matches clicked tab's data-tab value
      const targetTab = tab.getAttribute('data-tab');
      const matchingPanel = Array.from(panels).find(p => p.getAttribute('data-tab') === targetTab || p.getAttribute('data-panel') === targetTab);
      
      // e. Add "active" to matching panel
      if (matchingPanel) {
        matchingPanel.classList.add('active');
      }
      
      // f. Scroll smoothly to id="user-guide" with 80px offset
      const targetElement = document.getElementById('user-guide');
      if (targetElement) {
        const elementPosition = targetElement.getBoundingClientRect().top;
        const offsetPosition = elementPosition + window.pageYOffset - 80;
        window.scrollTo({
          top: offsetPosition,
          behavior: 'smooth'
        });
      }
    });
  });

  // 4. On page load activate first tab (start) by default
  const startTab = Array.from(tabs).find(t => t.getAttribute('data-tab') === 'start');
  if (startTab) {
    tabs.forEach(t => t.classList.remove('active'));
    panels.forEach(p => p.classList.remove('active'));
    
    startTab.classList.add('active');
    const startPanel = Array.from(panels).find(p => p.getAttribute('data-tab') === 'start' || p.getAttribute('data-panel') === 'start');
    if (startPanel) {
      startPanel.classList.add('active');
    }
  }

  // 5. Add copy button functionality to all elements with class "step-code"
  const codeBlocks = document.querySelectorAll('.step-code');
  codeBlocks.forEach(codeBlock => {
    let copyBtn = codeBlock.querySelector('.copy-btn');
    if (!copyBtn) {
      copyBtn = document.createElement('button');
      copyBtn.className = 'copy-btn';
      copyBtn.textContent = 'Copy';
      codeBlock.appendChild(copyBtn);
    }

    copyBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      
      // Read innerText of parent step-code block
      const rawText = codeBlock.innerText;
      
      // Remove the button text before copying
      let textToCopy = rawText;
      if (textToCopy.endsWith("Copied!")) {
        textToCopy = textToCopy.slice(0, -7);
      } else if (textToCopy.endsWith("Copy")) {
        textToCopy = textToCopy.slice(0, -4);
      }
      textToCopy = textToCopy.trim();

      // Write to clipboard via navigator.clipboard
      navigator.clipboard.writeText(textToCopy).then(() => {
        // Change button text to "Copied!"
        copyBtn.textContent = 'Copied!';
        // Add class "copied" to button
        copyBtn.classList.add('copied');
        
        // After 2000ms revert text to "Copy" and remove "copied" class
        setTimeout(() => {
          copyBtn.textContent = 'Copy';
          copyBtn.classList.remove('copied');
        }, 2000);
      });
    });
  });
}
