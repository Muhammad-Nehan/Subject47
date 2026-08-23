import { useRef } from 'react';
import * as THREE from 'three';
import { useFrame } from '@react-three/fiber';
import { Box, PerspectiveCamera, OrbitControls, useHelper } from '@react-three/drei';
import { EffectComposer, Bloom, Vignette, Noise, Scanline, ChromaticAberration } from '@react-three/postprocessing';
import { motion } from 'motion/react';

// Voxel Box with slight bevel/gap to emphasize individual blocks
export function Voxel({ position, color = '#111', scale = [1, 1, 1], ...props }: any) {
  return (
    <mesh position={position} scale={scale} {...props}>
      <boxGeometry args={[0.98, 0.98, 0.98]} />
      <meshStandardMaterial color={color} roughness={0.9} metalness={0.1} />
    </mesh>
  );
}

// Emissive Voxel for lights/screens
export function EmissiveVoxel({ position, color = '#ff0000', intensity = 2, ...props }: any) {
  return (
    <mesh position={position} {...props}>
      <boxGeometry args={[0.95, 0.95, 0.95]} />
      <meshStandardMaterial 
        color={color} 
        emissive={color} 
        emissiveIntensity={intensity} 
        toneMapped={false}
      />
    </mesh>
  );
}

// Modular Wall Segment
export function WallSegment({ position, rotation = [0, 0, 0] }: any) {
  return (
    <group position={position} rotation={rotation}>
      {/* 5x5 wall of voxels */}
      {Array.from({ length: 5 }).map((_, x) => 
        Array.from({ length: 5 }).map((_, y) => (
          <Voxel key={`${x}-${y}`} position={[x - 2, y, 0]} color="#0a0a0a" />
        ))
      )}
      {/* Decorative stripe */}
      <EmissiveVoxel position={[0, 2.5, 0.1]} scale={[5, 0.1, 0.1]} color="#112244" intensity={0.5} />
    </group>
  );
}

// Floor Segment
export function FloorSegment({ position }: any) {
  return (
    <group position={position}>
      {Array.from({ length: 5 }).map((_, x) => 
        Array.from({ length: 5 }).map((_, z) => (
          <Voxel key={`${x}-${z}`} position={[x - 2, -0.5, z - 2]} color={Math.random() > 0.9 ? "#111" : "#050505"} />
        ))
      )}
    </group>
  );
}

// Laboratory Scene
export function LabScene() {
  const lightRef = useRef<THREE.PointLight>(null!);
  
  useFrame(({ clock }) => {
    // Flickering red light
    if (lightRef.current) {
      const time = clock.getElapsedTime();
      const flicker = Math.sin(time * 10) > 0.8 ? 2 : Math.sin(time * 50) > 0.5 ? 0.2 : 1;
      lightRef.current.intensity = flicker * 5;
    }
  });

  return (
    <>
      <color attach="background" args={['#050505']} />
      <fog attach="fog" args={['#050505', 5, 25]} />
      
      <ambientLight intensity={0.01} />
      
      {/* Red Emergency Light */}
      <pointLight 
        ref={lightRef}
        position={[0, 4, 0]} 
        color="#ff0000" 
        distance={15} 
        decay={2}
      />

      {/* Corridor details */}
      {Array.from({ length: 15 }).map((_, i) => (
        <group key={i} position={[0, 0, -i * 4]}>
          <FloorSegment position={[0, 0, 0]} />
          <WallSegment position={[-3, 0, 0]} rotation={[0, Math.PI / 2, 0]} />
          <WallSegment position={[3, 0, 0]} rotation={[0, -Math.PI / 2, 0]} />
          
          {/* Floor Debris */}
          {i % 3 === 0 && (
             <Voxel 
               position={[Math.random() * 4 - 2, 0, Math.random() * 2 - 1]} 
               scale={[0.3, 0.1, 0.3]} 
               color="#333" 
             />
          )}

          {/* Wall Cables */}
          <EmissiveVoxel position={[-2.9, 3, 0]} scale={[0.1, 0.1, 4]} color="#111" />
          <EmissiveVoxel position={[-2.95, 2.8, 0]} scale={[0.05, 0.05, 4]} color="#050505" />

          {/* Ceiling lights (flickering blue/red) */}
          {i % 4 === 0 ? (
             <group position={[0, 4.5, 0]}>
                <EmissiveVoxel scale={[1, 0.1, 1]} color="#0088ff" intensity={8} />
                <pointLight color="#0088ff" intensity={2} distance={8} />
             </group>
          ) : i % 7 === 0 ? (
             <group position={[0, 4.5, 0]}>
                <EmissiveVoxel scale={[1, 0.1, 1]} color="#ff0000" intensity={12} />
                <pointLight color="#ff0000" intensity={3} distance={10} />
             </group>
          ) : null}

          {/* Crates */}
          {i === 2 && <Voxel position={[2, 0.5, 0]} scale={[1, 1, 1]} color="#222" />}
          {i === 5 && <Voxel position={[-2, 1.5, 0]} scale={[1, 1, 1]} color="#1a1a1a" />}
          {i === 5 && <Voxel position={[-2, 0.5, 0]} scale={[1, 1, 1]} color="#252525" />}
        </group>
      ))}

      {/* Lab Station (The discovery point) */}
      <group position={[0, 0, -25]}>
        <Voxel position={[0, 1, 0]} scale={[2, 2, 2]} color="#333" />
        <EmissiveVoxel position={[0, 1, 1.1]} scale={[1.5, 1, 0.1]} color="#00ffff" intensity={2} />
        
        {/* Shattered Containment Tank */}
        <group position={[0, 0, -5]}>
           {/* Base */}
           <Voxel position={[0, 0, 0]} scale={[4, 1, 4]} color="#222" />
           {/* "Glass" fragments (voxels) */}
           {Array.from({ length: 12 }).map((_, i) => (
              <Voxel 
                key={i} 
                position={[Math.random() * 4 - 2, 0.2, Math.random() * 4 - 2]} 
                scale={[0.2, 0.2, 0.2]} 
                rotation={[Math.random(), Math.random(), Math.random()]}
                color="#88ffff"
              />
           ))}
           {/* Label Terminal */}
           <Voxel position={[2.5, 1, -1]} scale={[0.5, 2, 0.5]} />
           <EmissiveVoxel position={[2.5, 2, -1]} scale={[0.6, 0.4, 0.1]} color="#ff0000" intensity={5} />
        </group>
      </group>

      <PostEffects />
    </>
  );
}

function PostEffects() {
  return (
    <EffectComposer>
      <Bloom 
        intensity={1.5} 
        luminanceThreshold={0.1} 
        luminanceSmoothing={0.9} 
      />
      <Vignette eskil={false} offset={0.1} darkness={0.8} />
      <Noise opacity={0.05} />
      <Scanline opacity={0.02} />
      <ChromaticAberration offset={new THREE.Vector2(0.001, 0.001)} />
    </EffectComposer>
  );
}
