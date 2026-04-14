//> Hash Tables table-c
#include <stdlib.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "table.h"
#include "value.h"

//> max-load
#define TABLE_MAX_LOAD 0.75

//< max-load
void initTable(Table* table) {
  table->count = 0;
  table->capacity = 0;
  table->entries = NULL;
}
//> free-table
void freeTable(Table* table) {
  FREE_ARRAY(Entry, table->entries, table->capacity);
  initTable(table);
}
//< free-table
//> find-entry
//> omit
// NOTE: The "Optimization" chapter has a manual copy of this function.
// If you change it here, make sure to update that copy.
//< omit

// Hash different value types
static uint32_t hashValue(Value key) {
  // Use nan-boxing macros
  if (IS_NIL(key)) return 0;
  if (IS_BOOL(key)) return AS_BOOL(key) ? 1 : 0;
  if (IS_NUMBER(key)) {
    double num = AS_NUMBER(key);
    uint64_t bits;
    memcpy(&bits, &num, sizeof(bits));
    return (uint32_t)(bits ^ (bits >> 32));
  }
  if (IS_OBJ(key)) {
    Obj* obj = AS_OBJ(key);
    // Use the hash field you added to Obj struct
    if (obj->hash != 0) return obj->hash;
    return (uint32_t)(uintptr_t)obj;
  }
  return 0;
}

static Entry* findEntry(Entry* entries, int capacity, Value key) {
  uint32_t hash = hashValue(key);
  uint32_t index = hash & (capacity - 1);
  Entry* tombstone = NULL;
  
  for (;;) {
    Entry* entry = &entries[index];
    if (IS_NIL(entry->key)) {  // Check for NIL, not NULL
      if (IS_NIL(entry->value)) {
        // Empty entry
        return tombstone != NULL ? tombstone : entry;
      } else {
        // Tombstone
        if (tombstone == NULL) tombstone = entry;
      }
    } else if (keysEqual(entry->key, key)) {
      return entry;
    }
    index = (index + 1) & (capacity - 1);
  }
}

// Compare keys for equality
static bool keysEqual(Value a, Value b) {
  // First handle primitive types using nan-boxing
  if (IS_NIL(a) && IS_NIL(b)) return true;
  if (IS_BOOL(a) && IS_BOOL(b)) return AS_BOOL(a) == AS_BOOL(b);
  if (IS_NUMBER(a) && IS_NUMBER(b)) return AS_NUMBER(a) == AS_NUMBER(b);
  
  // Handle object types
  if (IS_OBJ(a) && IS_OBJ(b)) {
    Obj* objA = AS_OBJ(a);
    Obj* objB = AS_OBJ(b);
    
    // For strings, compare content
    if (objA->type == OBJ_STRING && objB->type == OBJ_STRING) {
      ObjString* strA = (ObjString*)objA;
      ObjString* strB = (ObjString*)objB;
      return strA->length == strB->length && 
             memcmp(strA->chars, strB->chars, strA->length) == 0;
    }
    // For other objects, identity comparison
    return objA == objB;
  }
  
  return false;
}
//> table-get
bool tableGet(Table* table, Value key, Value* value) {
  if (table->count == 0) return false;
  
  Entry* entry = findEntry(table->entries, table->capacity, key);
  if (IS_NIL(entry->key)) return false;
  
  *value = entry->value;
  return true;
}
//< table-get
//> table-adjust-capacity
static void adjustCapacity(Table* table, int capacity) {
  Entry* entries = ALLOCATE(Entry, capacity);
  for (int i = 0; i < capacity; i++) {
    entries[i].key = NIL_VAL;
    entries[i].value = NIL_VAL;
  }
  
  table->count = 0;
  for (int i = 0; i < table->capacity; i++) {
    Entry* entry = &table->entries[i];
    if (IS_NIL(entry->key)) continue;  // Check for NIL
    
    Entry* dest = findEntry(entries, capacity, entry->key);
    dest->key = entry->key;
    dest->value = entry->value;
    table->count++;
  }
  
  FREE_ARRAY(Entry, table->entries, table->capacity);
  table->entries = entries;
  table->capacity = capacity;
}
//< table-adjust-capacity
//> table-set
bool tableSet(Table* table, Value key, Value value) {
  if (table->count + 1 > table->capacity * TABLE_MAX_LOAD) {
    int capacity = GROW_CAPACITY(table->capacity);
    adjustCapacity(table, capacity);
  }
  
  Entry* entry = findEntry(table->entries, table->capacity, key);
  bool isNewKey = IS_NIL(entry->key);  // Check for NIL
  if (isNewKey && IS_NIL(entry->value)) table->count++;
  
  entry->key = key;
  entry->value = value;
  return isNewKey;
}
//< table-set
//> table-delete
bool tableDelete(Table* table, Value key) {
  if (table->count == 0) return false;
  
  Entry* entry = findEntry(table->entries, table->capacity, key);
  if (IS_NIL(entry->key)) return false;
  
  // Place a tombstone
  entry->key = NIL_VAL;
  entry->value = BOOL_VAL(true);
  return true;
}
//< table-delete
//> table-add-all
void tableAddAll(Table* from, Table* to) {
  for (int i = 0; i < from->capacity; i++) {
    Entry* entry = &from->entries[i];
    if (!IS_NIL(entry->key)) {
      tableSet(to, entry->key, entry->value);
    }
  }
}
//< table-add-all
//> table-find-string
ObjString* tableFindString(Table* table, const char* chars,
                           int length, uint32_t hash) {
  if (table->count == 0) return NULL;
  
  uint32_t index = hash & (table->capacity - 1);
  for (;;) {
    Entry* entry = &table->entries[index];
    if (IS_NIL(entry->key)) {
      if (IS_NIL(entry->value)) return NULL;
    } else if (IS_STRING(entry->key)) {  // Check if it's a string key
      ObjString* key = AS_STRING(entry->key);
      if (key->length == length && key->hash == hash &&
          memcmp(key->chars, chars, length) == 0) {
        return key;
      }
    }
    index = (index + 1) & (table->capacity - 1);
  }
}
//< table-find-string
//> Garbage Collection table-remove-white
void tableRemoveWhite(Table* table) {
  for (int i = 0; i < table->capacity; i++) {
    Entry* entry = &table->entries[i];
    // Check if key is an object and not marked
    if (!IS_NIL(entry->key) && IS_OBJ(entry->key)) {
      Obj* obj = AS_OBJ(entry->key);
      if (!obj->isMarked) {
        tableDelete(table, entry->key);
      }
    }
  }
}
//< Garbage Collection table-remove-white
//> Garbage Collection mark-table
void markTable(Table* table) {
  for (int i = 0; i < table->capacity; i++) {
    Entry* entry = &table->entries[i];
    if (!IS_NIL(entry->key)) { 
      markValue(entry->key);    
      markValue(entry->value);
    }
  }
}
//< Garbage Collection mark-table
