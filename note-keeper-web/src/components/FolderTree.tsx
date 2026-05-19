/**
 * @module FolderTree
 * @category Components
 * Recursive tree component that renders a {@link FolderStructure} hierarchy.
 *
 * - Nodes with children show a chevron toggle button (expand / collapse).
 * - The currently selected folder is highlighted with the primary colour.
 * - Hovering a folder reveals a `+` button for inline sub-folder creation.
 * - New folders are created by typing a name and pressing `Enter` or blurring
 *   the input; `Escape` cancels.
 */
import React, { useState } from 'react';
import { FolderStructure } from '../types';

/** Props for {@link FolderTree}. */
interface FolderTreeProps {
  /**
   * Root-level folder nodes to render.  Typically a single-element array
   * containing the `"All Notes"` root returned by {@link buildFolderTree}.
   */
  folders: FolderStructure[];
  /** Path of the currently selected folder (highlights the matching node). */
  selectedPath: string;
  /** Called when the user clicks a folder node. */
  onSelectFolder: (path: string) => void;
  /**
   * Called when the user creates a new sub-folder.
   * @param parentPath - Path of the parent folder.
   * @param name - Name for the new sub-folder.
   */
  onCreateFolder: (parentPath: string, name: string) => void;
}

/**
 * Recursive folder tree navigation component.
 * @param props - See {@link FolderTreeProps}.
 */
export const FolderTree: React.FC<FolderTreeProps> = ({
  folders,
  selectedPath,
  onSelectFolder,
  onCreateFolder
}) => {
  const [expandedFolders, setExpandedFolders] = useState<Set<string>>(new Set(['root']));
  const [creatingFolder, setCreatingFolder] = useState<string | null>(null);
  const [newFolderName, setNewFolderName] = useState('');

  const toggleFolder = (path: string) => {
    const newExpanded = new Set(expandedFolders);
    if (newExpanded.has(path)) {
      newExpanded.delete(path);
    } else {
      newExpanded.add(path);
    }
    setExpandedFolders(newExpanded);
  };

  const handleCreateFolder = (parentPath: string) => {
    if (newFolderName.trim()) {
      onCreateFolder(parentPath, newFolderName.trim());
      setNewFolderName('');
      setCreatingFolder(null);
    }
  };

  const renderFolder = (folder: FolderStructure, level: number = 0) => {
    const isExpanded = expandedFolders.has(folder.path);
    const isSelected = selectedPath === folder.path;
    const hasSubfolders = folder.subfolders.length > 0;

    return (
      <div key={folder.path}>
        <div
          className={`flex items-center gap-2 px-3 py-2 rounded-lg cursor-pointer transition-colors ${
            isSelected ? 'bg-primary text-white' : 'hover:bg-gray-100'
          }`}
          style={{ paddingLeft: `${level * 16 + 12}px` }}
        >
          {hasSubfolders && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                toggleFolder(folder.path);
              }}
              className="w-4 h-4 flex items-center justify-center"
            >
              <i className={`fas fa-chevron-${isExpanded ? 'down' : 'right'} text-xs`}></i>
            </button>
          )}
          {!hasSubfolders && <div className="w-4"></div>}
          <i className={`fas ${isExpanded ? 'fa-folder-open' : 'fa-folder'} text-sm`}></i>
          <span
            className="flex-1 text-sm font-medium"
            onClick={() => onSelectFolder(folder.path)}
          >
            {folder.name}
          </span>
          <button
            onClick={(e) => {
              e.stopPropagation();
              setCreatingFolder(folder.path);
            }}
            className="opacity-0 group-hover:opacity-100 hover:text-primary"
          >
            <i className="fas fa-plus text-xs"></i>
          </button>
        </div>

        {creatingFolder === folder.path && (
          <div
            className="flex items-center gap-2 px-3 py-2"
            style={{ paddingLeft: `${(level + 1) * 16 + 12}px` }}
          >
            <i className="fas fa-folder text-sm text-gray-400"></i>
            <input
              type="text"
              value={newFolderName}
              onChange={(e) => setNewFolderName(e.target.value)}
              onKeyPress={(e) => {
                if (e.key === 'Enter') handleCreateFolder(folder.path);
                if (e.key === 'Escape') setCreatingFolder(null);
              }}
              onBlur={() => {
                if (newFolderName.trim()) handleCreateFolder(folder.path);
                else setCreatingFolder(null);
              }}
              className="flex-1 text-sm px-2 py-1 border border-primary rounded focus:outline-none"
              placeholder="Folder name"
              autoFocus
            />
          </div>
        )}

        {isExpanded && hasSubfolders && (
          <div>
            {folder.subfolders.map(subfolder => renderFolder(subfolder, level + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-1 group">
      {folders.map(folder => renderFolder(folder))}
    </div>
  );
};
