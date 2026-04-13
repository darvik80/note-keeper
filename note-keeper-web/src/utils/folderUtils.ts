import { FolderStructure, Note } from '../types';

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

export const getFullPath = (folder: string, subfolder?: string): string => {
  if (!subfolder) return folder;
  return `${folder}/${subfolder}`;
};

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
