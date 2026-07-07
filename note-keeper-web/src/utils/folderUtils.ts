/**
 * @module folderUtils
 * @category Utils
 * Utilities for converting flat note `folder`/`subfolder` fields into a
 * hierarchical {@link FolderStructure} tree and for round-tripping between
 * path strings and `{ folder, subfolder }` pairs.
 */
import {FolderStructure, Note} from '../types';

/**
 * Build a hierarchical folder tree from a flat list of notes.
 *
 * The function reads each note's `folder` and `subfolder` fields, splits them
 * on `"/"`, and assembles a tree rooted at a virtual `"root"` node labelled
 * `"All Notes"`.
 *
 * **Algorithm:**
 * 1. Seed the map with the root node.
 * 2. For each note, walk the folder path segments and create intermediate nodes
 *    as needed, linking each node to its parent's `subfolders` array.
 * 3. If the note has a `subfolder`, repeat the same walk relative to the
 *    note's top-level `folder`.
 *
 * Duplicate paths are de-duplicated automatically (the map acts as an identity
 * cache keyed by path string).
 *
 * @param notes - The full list of notes (archived, deleted, etc. included).
 * @returns A single-element array containing the root {@link FolderStructure}
 *   node.  The tree is ready to pass to {@link FolderTree}.
 *
 * @example
 * ```ts
 * const tree = buildFolderTree(notes);
 * // tree[0].name === 'All Notes'
 * // tree[0].subfolders[0].path === 'Work'
 * ```
 */
export const buildFolderTree = (notes: Note[]): FolderStructure[] => {
  const folderMap = new Map<string, FolderStructure>();

  folderMap.set('root', {
    name: 'All Notes',
    path: 'root',
    subfolders: []
  });

  notes.forEach(note => {
    const parts = note.folder.split('/').filter(p => p);
    let currentPath = 'root';

    parts.forEach((part, index) => {
      const parentPath = currentPath;
      currentPath = index === 0 ? part : `${currentPath}/${part}`;

      if (!folderMap.has(currentPath)) {
        const folder: FolderStructure = {
          name: part,
          path: currentPath,
          subfolders: []
        };
        folderMap.set(currentPath, folder);

        const parent = folderMap.get(parentPath);
        if (parent && !parent.subfolders.find(f => f.path === currentPath)) {
          parent.subfolders.push(folder);
        }
      }
    });

    if (note.subfolder) {
      const subParts = note.subfolder.split('/').filter(p => p);
      let subPath = note.folder;

      subParts.forEach(part => {
        const parentPath = subPath;
        subPath = `${subPath}/${part}`;

        if (!folderMap.has(subPath)) {
          const folder: FolderStructure = {
            name: part,
            path: subPath,
            subfolders: []
          };
          folderMap.set(subPath, folder);

          const parent = folderMap.get(parentPath);
          if (parent && !parent.subfolders.find(f => f.path === subPath)) {
            parent.subfolders.push(folder);
          }
        }
      });
    }
  });

  return [folderMap.get('root')!];
};

/**
 * Combine a `folder` and an optional `subfolder` into a single slash-separated
 * path string compatible with {@link FolderStructure.path}.
 *
 * @param folder - Top-level folder name (e.g. `"Work"`).
 * @param subfolder - Optional nested folder (e.g. `"Q1"`).
 * @returns Combined path (e.g. `"Work/Q1"`) or just `folder` when no subfolder.
 */
export const getFullPath = (folder: string, subfolder?: string): string => {
  if (!subfolder) return folder;
  return `${folder}/${subfolder}`;
};

/**
 * Parse a {@link FolderStructure.path} string back into `{ folder, subfolder }`.
 *
 * The special path `"root"` maps to `{ folder: "default" }` — the default
 * folder used for new notes when no folder is selected.
 *
 * @param path - Slash-separated folder path from the folder tree.
 * @returns An object with `folder` and an optional `subfolder`.
 *
 * @example
 * ```ts
 * parsePath('Work/Q1') // → { folder: 'Work', subfolder: 'Q1' }
 * parsePath('Work')    // → { folder: 'Work' }
 * parsePath('root')    // → { folder: 'default' }
 * ```
 */
export const parsePath = (path: string): { folder: string; subfolder?: string } => {
  if (path === 'root') return { folder: 'default' };

  const parts = path.split('/');
  if (parts.length === 1) {
    return { folder: parts[0] };
  }

  return {
    folder: parts[0],
    subfolder: parts.slice(1).join('/')
  };
};
