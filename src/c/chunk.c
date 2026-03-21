//> Chunks of Bytecode chunk-c
#include <stdlib.h>
#include <stdio.h>
#include "chunk.h"
//> chunk-c-include-memory
#include "memory.h"
//< chunk-c-include-memory
//> Garbage Collection chunk-include-vm
#include "vm.h"
//< Garbage Collection chunk-include-vm



void initChunk(Chunk* chunk) {
  chunk->count = 0;
  chunk->capacity = 0;
  chunk->code = NULL;
//> chunk-null-lines
  chunk->lineNumbers = NULL;
  chunk->lineCounts = NULL;
  chunk->lineCapacity = 0;
  chunk->lineCount = 0;
//< chunk-null-lines
//> chunk-init-constant-array
  initValueArray(&chunk->constants);
//< chunk-init-constant-array
}
//> free-chunk
void freeChunk(Chunk* chunk) {
  FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
//> chunk-free-lines
  FREE_ARRAY(int, chunk->lineNumbers, chunk->lineCapacity);
  FREE_ARRAY(int, chunk->lineCounts, chunk->lineCapacity);
//< chunk-free-lines
//> chunk-free-constants
  freeValueArray(&chunk->constants);
//< chunk-free-constants
  initChunk(chunk);
}
//< free-chunk
/* Chunks of Bytecode write-chunk < Chunks of Bytecode write-chunk-with-line
void writeChunk(Chunk* chunk, uint8_t byte) {
*/
//> write-chunk
//> write-chunk-with-line
void writeChunk(Chunk* chunk, uint8_t byte, int line) {
//< write-chunk-with-line
  if (chunk->capacity < chunk->count + 1) {
    int oldCapacity = chunk->capacity;
    chunk->capacity = GROW_CAPACITY(oldCapacity);
    chunk->code = GROW_ARRAY(uint8_t, chunk->code,
        oldCapacity, chunk->capacity);
  }

  chunk->code[chunk->count] = byte;
  chunk->count++;

  //> chunk-write-line
  // Run-length encode the line information
  if (chunk->lineCount > 0 && 
      chunk->lineNumbers[chunk->lineCount - 1] == line) {
    // Same line as previous instruction, just increment count
    chunk->lineCounts[chunk->lineCount - 1]++;
  } else {
    // New line, add a new run
    if (chunk->lineCapacity < chunk->lineCount + 1) {
      int oldCapacity = chunk->lineCapacity;
      chunk->lineCapacity = GROW_CAPACITY(oldCapacity);
      chunk->lineNumbers = GROW_ARRAY(int, chunk->lineNumbers,
          oldCapacity, chunk->lineCapacity);
      chunk->lineCounts = GROW_ARRAY(int, chunk->lineCounts,
          oldCapacity, chunk->lineCapacity);
    }
    chunk->lineNumbers[chunk->lineCount] = line;
    chunk->lineCounts[chunk->lineCount] = 1;
    chunk->lineCount++;
  }
//< chunk-write-line
}
//< write-chunk
//> add-constant
int addConstant(Chunk* chunk, Value value) {
//> Garbage Collection add-constant-push
  push(value);
//< Garbage Collection add-constant-push
  writeValueArray(&chunk->constants, value);
//> Garbage Collection add-constant-pop
  pop();
//< Garbage Collection add-constant-pop
  return chunk->constants.count - 1;
}
//< add-constant

//> get-line
int getLine(Chunk* chunk, int instruction) {
  int count = 0;
  for (int i = 0; i < chunk->lineCount; i++) {
    count += chunk->lineCounts[i];
    if (instruction < count) {
      return chunk->lineNumbers[i];
    }
  }
  return -1;  // Should never reach here
}
//< get-line

void writeConstant(Chunk* chunk, Value value, int line) {
  int index = addConstant(chunk, value);
  
  if (index < 256) {
    // Use single-byte OP_CONSTANT
    writeChunk(chunk, OP_CONSTANT, line);
    writeChunk(chunk, (uint8_t)index, line);
  } else if (index < 16777216) {  // 2^24
    // Use three-byte OP_CONSTANT_LONG
    writeChunk(chunk, OP_CONSTANT_LONG, line);
    // Write 24-bit index in little-endian order
    writeChunk(chunk, (uint8_t)(index & 0xff), line);
    writeChunk(chunk, (uint8_t)((index >> 8) & 0xff), line);
    writeChunk(chunk, (uint8_t)((index >> 16) & 0xff), line);
  } else {
    // Too many constants!
    fprintf(stderr, "Too many constants in one chunk.\n");
    exit(1);
  }
}