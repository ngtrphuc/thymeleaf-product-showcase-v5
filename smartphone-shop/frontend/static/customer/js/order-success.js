document.addEventListener('DOMContentLoaded', () => {
    const stage = document.getElementById('success-celebration');
    if (!stage) return;

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    if (!document.getElementById('success-confetti-style')) {
        const style = document.createElement('style');
        style.id = 'success-confetti-style';
        style.textContent = `
            #success-celebration {
                position: fixed;
                inset: 0;
                pointer-events: none;
                overflow: hidden;
                z-index: 8;
            }
            .success-confetti-piece {
                position: fixed;
                left: calc(var(--start-x) * 1vw);
                top: calc(var(--start-y) * 1vh);
                width: var(--w);
                height: var(--h);
                background: var(--color);
                border-radius: var(--radius);
                opacity: 0;
                transform: translate3d(0, 0, 0) rotate(var(--rot-start));
                animation: success-confetti-burst var(--duration) cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
                will-change: transform, opacity;
            }
            @keyframes success-confetti-burst {
                0% {
                    opacity: 0;
                    transform: translate3d(0, 0, 0) rotate(var(--rot-start));
                }
                10% {
                    opacity: 1;
                }
                52% {
                    opacity: 1;
                    transform: translate3d(var(--peak-x), var(--peak-y), 0) rotate(var(--rot-mid));
                }
                100% {
                    opacity: 0;
                    transform: translate3d(var(--end-x), var(--end-y), 0) rotate(var(--rot-end));
                }
            }
        `;
        document.head.appendChild(style);
    }

    const colors = ['#000000', '#212121', '#8c8c8c', '#d4b48a', '#f1e3d1'];

    const random = (min, max) => min + Math.random() * (max - min);

    const spawnPiece = (fromLeft) => {
        const piece = document.createElement('span');
        piece.className = 'success-confetti-piece';

        const startX = fromLeft ? random(8, 16) : random(84, 92);
        const startY = random(76, 86);

        const peakX = fromLeft ? random(14, 38) : -random(14, 38);
        const endX = fromLeft ? random(20, 56) : -random(20, 56);
        const peakY = -random(30, 60);
        const endY = random(12, 42);

        piece.style.setProperty('--start-x', startX.toFixed(2));
        piece.style.setProperty('--start-y', startY.toFixed(2));
        piece.style.setProperty('--peak-x', `${peakX.toFixed(2)}vw`);
        piece.style.setProperty('--peak-y', `${peakY.toFixed(2)}vh`);
        piece.style.setProperty('--end-x', `${endX.toFixed(2)}vw`);
        piece.style.setProperty('--end-y', `${endY.toFixed(2)}vh`);
        piece.style.setProperty('--duration', `${random(1650, 2650).toFixed(0)}ms`);
        piece.style.setProperty('--rot-start', `${random(-90, 90).toFixed(0)}deg`);
        piece.style.setProperty('--rot-mid', `${random(-280, 280).toFixed(0)}deg`);
        piece.style.setProperty('--rot-end', `${random(-540, 540).toFixed(0)}deg`);
        piece.style.setProperty('--w', `${random(5, 11).toFixed(0)}px`);
        piece.style.setProperty('--h', `${random(10, 20).toFixed(0)}px`);
        piece.style.setProperty('--radius', Math.random() > 0.68 ? '999px' : `${random(0, 3).toFixed(0)}px`);
        piece.style.setProperty('--color', colors[Math.floor(Math.random() * colors.length)]);

        stage.appendChild(piece);
        piece.addEventListener('animationend', () => piece.remove(), { once: true });
    };

    const burst = () => {
        for (let i = 0; i < 26; i++) {
            window.setTimeout(() => spawnPiece(true), Math.random() * 220);
            window.setTimeout(() => spawnPiece(false), Math.random() * 220);
        }
    };
    burst();
    window.setTimeout(burst, 420);
    window.setTimeout(burst, 920);
});

