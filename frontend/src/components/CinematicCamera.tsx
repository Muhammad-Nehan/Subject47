import { useRef, useMemo } from 'react';
import * as THREE from 'three';
import { useFrame } from '@react-three/fiber';
import { PerspectiveCamera } from '@react-three/drei';

type CameraPathPoint = {
  time: number;
  pos: [number, number, number];
  lookAt: [number, number, number];
};

const CAMERA_PATH: CameraPathPoint[] = [
  { time: 0, pos: [0, 2, 5], lookAt: [0, 1.5, -5] },        // Start: Corridor entrance
  { time: 5, pos: [0, 2, -10], lookAt: [0, 1.5, -20] },    // 5s: Moving forward
  { time: 8, pos: [-2, 3, -15], lookAt: [1, 1.5, -25] },   // 8s: Viewing station from side
  { time: 13, pos: [0, 2, -28], lookAt: [0, 1, -31] },     // 13s: Zooming into shattered tank
  { time: 16, pos: [2, 2.2, -26], lookAt: [2.5, 2, -26] }, // 16s: Terminal focus
  { time: 20, pos: [2.3, 2.1, -26.3], lookAt: [2.5, 2, -26] } // end
];

export function CinematicCamera({ progress }: { progress: number }) {
  const cameraRef = useRef<THREE.PerspectiveCamera>(null!);
  
  // Find current segment in path
  const currentSegment = useMemo(() => {
    const timeInSeconds = progress * 20; // 20s total duration
    for (let i = 0; i < CAMERA_PATH.length - 1; i++) {
        if (timeInSeconds >= CAMERA_PATH[i].time && timeInSeconds <= CAMERA_PATH[i + 1].time) {
            const duration = CAMERA_PATH[i+1].time - CAMERA_PATH[i].time;
            const t = (timeInSeconds - CAMERA_PATH[i].time) / duration;
            return { start: CAMERA_PATH[i], end: CAMERA_PATH[i+1], t };
        }
    }
    return { start: CAMERA_PATH[CAMERA_PATH.length - 1], end: CAMERA_PATH[CAMERA_PATH.length - 1], t: 0 };
  }, [progress]);

  useFrame(() => {
    if (cameraRef.current) {
        const { start, end, t } = currentSegment;
        
        // Eased interpolation
        const easedT = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;

        // Position interpolation
        cameraRef.current.position.set(
            THREE.MathUtils.lerp(start.pos[0], end.pos[0], easedT),
            THREE.MathUtils.lerp(start.pos[1], end.pos[1], easedT),
            THREE.MathUtils.lerp(start.pos[2], end.pos[2], easedT)
        );

        // LookAt interpolation
        const lookX = THREE.MathUtils.lerp(start.lookAt[0], end.lookAt[0], easedT);
        const lookY = THREE.MathUtils.lerp(start.lookAt[1], end.lookAt[1], easedT);
        const lookZ = THREE.MathUtils.lerp(start.lookAt[2], end.lookAt[2], easedT);
        cameraRef.current.lookAt(lookX, lookY, lookZ);
    }
  });

  return (
    <PerspectiveCamera 
      ref={cameraRef} 
      makeDefault 
      fov={50} 
      near={0.1} 
      far={100} 
    />
  );
}
