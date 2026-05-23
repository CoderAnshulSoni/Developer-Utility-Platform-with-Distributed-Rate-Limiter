// DevPlatform System Architecture Diagram
// Built using HTML5 Canvas API with animated data flows, interactive hovers, and high-fidelity styling.

document.addEventListener('DOMContentLoaded', () => {
  initArchitectureDiagram();
});

function initArchitectureDiagram() {
  const canvas = document.getElementById('arch-diagram');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  
  // Set fixed internal coordinate space
  canvas.width = 1000;
  canvas.height = 500;

  // State Management
  let isPlaying = true;
  let dashOffset = 0;
  let hoveredComponent = null;
  let mouseX = 0;
  let mouseY = 0;

  // Component Nodes Configuration
  const components = [
    {
      id: "react-ui",
      name: "React UI",
      emoji: "💻",
      port: ":3000",
      tech: "React, CSS, Axios",
      desc: "Client dashboard hosting SQL analysis visualizers, execution-plan visual breakdown, API sandbox, and live telemetry audit logs.",
      x: 500,
      y: 90,
      w: 220,
      h: 100
    },
    {
      id: "dev-toolkit",
      name: "Dev Toolkit",
      emoji: "🛠️",
      port: ":8080",
      tech: "Spring Boot, AOP, JPA",
      desc: "Core developer platform gateway executing SQL checks, Postgres explain-plans, secure sandboxed REST calls, and request intercept tracing.",
      x: 280,
      y: 250,
      w: 220,
      h: 100
    },
    {
      id: "rate-limiter",
      name: "Rate Limiter",
      emoji: "⚡",
      port: ":8081",
      tech: "Spring Boot, Redis Cache",
      desc: "High-throughput token bucket and sliding window rate limiter assessing transaction limits on endpoints dynamically.",
      x: 720,
      y: 250,
      w: 220,
      h: 100
    },
    {
      id: "postgres",
      name: "PostgreSQL",
      emoji: "🐘",
      port: ":5432",
      tech: "Relational Storage",
      desc: "Seeded relational database storing transaction records, execution history, and correlation logging metadata.",
      x: 280,
      y: 410,
      w: 220,
      h: 100
    },
    {
      id: "redis",
      name: "Redis Cache",
      emoji: "🔴",
      port: ":6379",
      tech: "In-Memory Store",
      desc: "Ultra-fast distributed cache holding moving request window limits and active sliding rate buckets securely.",
      x: 720,
      y: 410,
      w: 220,
      h: 100
    }
  ];

  // Animated Connections Configuration
  const connections = [
    {
      from: "react-ui",
      to: "dev-toolkit",
      label: "HTTP REST",
      type: "normal",
      color: "#00B4D8",
      dash: [6, 4],
      startX: 500,
      startY: 140,
      endX: 280,
      endY: 200,
      isBezier: true,
      cp1x: 500,
      cp1y: 170,
      cp2x: 280,
      cp2y: 170,
      midX: 390,
      midY: 170,
      arrowAngle: Math.PI / 2
    },
    {
      from: "dev-toolkit",
      to: "rate-limiter",
      label: "validate request",
      type: "normal",
      color: "#00B4D8",
      dash: [6, 4],
      startX: 390,
      startY: 235,
      endX: 610,
      endY: 235,
      isBezier: false,
      midX: 500,
      midY: 235,
      arrowAngle: 0
    },
    {
      from: "dev-toolkit",
      to: "rate-limiter",
      label: "fail-open if down",
      type: "fail-open",
      color: "#ef4444",
      dash: [4, 4],
      startX: 390,
      startY: 265,
      endX: 610,
      endY: 265,
      isBezier: false,
      midX: 500,
      midY: 265,
      arrowAngle: 0
    },
    {
      from: "dev-toolkit",
      to: "postgres",
      label: "persist data",
      type: "normal",
      color: "#00B4D8",
      dash: [6, 4],
      startX: 280,
      startY: 300,
      endX: 280,
      endY: 360,
      isBezier: false,
      midX: 280,
      midY: 330,
      arrowAngle: Math.PI / 2
    },
    {
      from: "rate-limiter",
      to: "redis",
      label: "read/write state",
      type: "normal",
      color: "#00B4D8",
      dash: [6, 4],
      startX: 720,
      startY: 300,
      endX: 720,
      endY: 360,
      isBezier: false,
      midX: 720,
      midY: 330,
      arrowAngle: Math.PI / 2
    }
  ];

  // Helper: Draw Rounded Rectangles
  function drawRoundedRect(c, rx, ry, rw, rh, rad, fill = true, stroke = true) {
    c.beginPath();
    c.moveTo(rx + rad, ry);
    c.lineTo(rx + rw - rad, ry);
    c.quadraticCurveTo(rx + rw, ry, rx + rw, ry + rad);
    c.lineTo(rx + rw, ry + rh - rad);
    c.quadraticCurveTo(rx + rw, ry + rh, rx + rw - rad, ry + rh);
    c.lineTo(rx + rad, ry + rh);
    c.quadraticCurveTo(rx, ry + rh, rx, ry + rh - rad);
    c.lineTo(rx, ry + rad);
    c.quadraticCurveTo(rx, ry, rx + rad, ry);
    c.closePath();
    if (fill) c.fill();
    if (stroke) c.stroke();
  }

  // Draw Arrowhead Helper
  function drawArrowhead(c, x, y, angle, color) {
    c.save();
    c.translate(x, y);
    c.rotate(angle);
    c.fillStyle = color;
    c.beginPath();
    c.moveTo(0, 0);
    c.lineTo(-10, -5);
    c.lineTo(-10, 5);
    c.closePath();
    c.fill();
    c.restore();
  }

  // Draw Connection Lines
  function drawConnections() {
    connections.forEach(conn => {
      // Draw Base solid faint pipeline background
      ctx.save();
      ctx.strokeStyle = conn.type === "fail-open" ? "rgba(239, 68, 68, 0.1)" : "rgba(0, 180, 216, 0.15)";
      ctx.lineWidth = conn.type === "fail-open" ? 2 : 3;
      
      if (conn.isBezier) {
        ctx.beginPath();
        ctx.moveTo(conn.startX, conn.startY);
        ctx.bezierCurveTo(conn.cp1x, conn.cp1y, conn.cp2x, conn.cp2y, conn.endX, conn.endY);
        ctx.stroke();
      } else {
        ctx.beginPath();
        ctx.moveTo(conn.startX, conn.startY);
        ctx.lineTo(conn.endX, conn.endY);
        ctx.stroke();
      }
      ctx.restore();

      // Draw Animated Dash Flow Layer
      ctx.save();
      ctx.strokeStyle = conn.color;
      ctx.lineWidth = conn.type === "fail-open" ? 1.5 : 2.5;
      ctx.setLineDash(conn.dash);
      ctx.lineDashOffset = dashOffset;

      if (conn.isBezier) {
        ctx.beginPath();
        ctx.moveTo(conn.startX, conn.startY);
        ctx.bezierCurveTo(conn.cp1x, conn.cp1y, conn.cp2x, conn.cp2y, conn.endX, conn.endY);
        ctx.stroke();
      } else {
        ctx.beginPath();
        ctx.moveTo(conn.startX, conn.startY);
        ctx.lineTo(conn.endX, conn.endY);
        ctx.stroke();
      }
      ctx.restore();

      // Draw Arrowheads at destination
      drawArrowhead(ctx, conn.endX, conn.endY, conn.arrowAngle, conn.color);

      // Draw connection label pill
      ctx.save();
      ctx.font = "600 11px Inter, sans-serif";
      const textW = ctx.measureText(conn.label).width;
      const pillW = textW + 16;
      const pillH = 18;

      ctx.fillStyle = "#0A1628";
      ctx.strokeStyle = conn.type === "fail-open" ? "rgba(239, 68, 68, 0.4)" : "rgba(255, 255, 255, 0.08)";
      ctx.lineWidth = 1;
      
      drawRoundedRect(ctx, conn.midX - pillW / 2, conn.midY - pillH / 2, pillW, pillH, 9, true, true);

      // Draw label text
      ctx.fillStyle = conn.type === "fail-open" ? "#ef4444" : "#94a3b8";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(conn.label, conn.midX, conn.midY);
      ctx.restore();
    });
  }

  // Draw Component Nodes
  function drawComponents() {
    components.forEach(comp => {
      const rx = comp.x - comp.w / 2;
      const ry = comp.y - comp.h / 2;
      const isHovered = hoveredComponent && hoveredComponent.id === comp.id;

      ctx.save();
      
      // Glow and Shadow highlight on hover
      if (isHovered) {
        ctx.shadowColor = "#00B4D8";
        ctx.shadowBlur = 20;
        ctx.strokeStyle = "#00B4D8";
      } else {
        ctx.shadowColor = "rgba(0, 0, 0, 0.4)";
        ctx.shadowBlur = 10;
        ctx.strokeStyle = "rgba(255, 255, 255, 0.1)";
      }

      // Draw Card Background Fill
      ctx.fillStyle = "#112240";
      ctx.lineWidth = 2;
      drawRoundedRect(ctx, rx, ry, comp.w, comp.h, 12, true, true);
      ctx.restore();

      // Draw Emojis (Top center of card)
      ctx.save();
      ctx.font = "24px sans-serif";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(comp.emoji, comp.x, ry + 24);
      ctx.restore();

      // Draw Service Name (Bold White)
      ctx.save();
      ctx.font = "bold 15px Outfit, sans-serif";
      ctx.fillStyle = "#ffffff";
      ctx.textAlign = "center";
      ctx.fillText(comp.name, comp.x, ry + 54);

      // Draw Port Number (Accent color)
      ctx.font = "bold 13px Outfit, sans-serif";
      ctx.fillStyle = "#00B4D8";
      const nameW = ctx.measureText(comp.name).width;
      // Draw port adjacent to service name
      ctx.textAlign = "left";
      ctx.fillText(comp.port, comp.x + nameW / 2 + 4, ry + 54);
      ctx.restore();

      // Draw Tech Stack Label (Small Muted below)
      ctx.save();
      ctx.font = "500 11px Inter, sans-serif";
      ctx.fillStyle = "#94a3b8";
      ctx.textAlign = "center";
      ctx.fillText(comp.tech, comp.x, ry + 78);
      ctx.restore();
    });
  }

  // Primary Draw Loop
  function tick() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw lines first so boxes render on top
    drawConnections();
    drawComponents();

    // Step dashes flow
    if (isPlaying) {
      dashOffset = (dashOffset - 0.7) % 100;
    }

    requestAnimationFrame(tick);
  }

  // Hover Bounding Box Detections
  function getMouseTarget(mx, my) {
    for (let comp of components) {
      const rx = comp.x - comp.w / 2;
      const ry = comp.y - comp.h / 2;
      if (mx >= rx && mx <= rx + comp.w && my >= ry && my <= ry + comp.h) {
        return comp;
      }
    }
    return null;
  }

  // Canvas Mouse Track Listeners
  canvas.addEventListener('mousemove', (e) => {
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    
    mouseX = (e.clientX - rect.left) * scaleX;
    mouseY = (e.clientY - rect.top) * scaleY;
    
    const target = getMouseTarget(mouseX, mouseY);
    if (target !== hoveredComponent) {
      hoveredComponent = target;
      canvas.style.cursor = target ? 'pointer' : 'default';
    }
  });

  canvas.addEventListener('mouseleave', () => {
    hoveredComponent = null;
    canvas.style.cursor = 'default';
  });

  // Play/Pause Interactive Controller Integration
  const toggleBtn = document.getElementById('toggle-animation-btn');
  const btnIcon = document.getElementById('anim-btn-icon');
  const btnText = document.getElementById('anim-btn-text');

  if (toggleBtn) {
    toggleBtn.addEventListener('click', () => {
      isPlaying = !isPlaying;
      if (btnIcon && btnText) {
        btnIcon.textContent = isPlaying ? "⏸" : "▶";
        btnText.textContent = isPlaying ? "Pause Animation" : "Resume Animation";
      }
      
      // Toggle secondary/primary state styling for button
      if (isPlaying) {
        toggleBtn.classList.remove('btn-primary');
        toggleBtn.classList.add('btn-secondary');
      } else {
        toggleBtn.classList.remove('btn-secondary');
        toggleBtn.classList.add('btn-primary');
      }
    });
  }

  // Start render loops
  tick();
}
