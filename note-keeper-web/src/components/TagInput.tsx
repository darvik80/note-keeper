import React, {useEffect, useRef, useState} from 'react';
import {api} from '../utils/api';

interface TagInputProps {
  tags: string[];
  onChange: (tags: string[]) => void;
}

/**
 * Reusable tag input with Add button (mobile-friendly) and autocomplete suggestions.
 * Fetches existing tags from backend on mount for autocomplete.
 */
export const TagInput: React.FC<TagInputProps> = ({ tags, onChange }) => {
  const [input, setInput] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [allTags, setAllTags] = useState<string[]>([]);
  const wrapperRef = useRef<HTMLDivElement>(null);

  // Load all tags once on mount
  useEffect(() => {
    let cancelled = false;
    api.tags.getAll()
      .then((loadedTags) => {
        if (!cancelled) setAllTags(loadedTags);
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const addTag = (tag: string) => {
    const trimmed = tag.trim();
    if (!trimmed || tags.includes(trimmed)) {
      setInput('');
      setShowSuggestions(false);
      return;
    }
    onChange([...tags, trimmed]);
    setInput('');
    setShowSuggestions(false);
  };

  const removeTag = (tag: string) => {
    onChange(tags.filter(t => t !== tag));
  };

  const handleInputChange = (value: string) => {
    setInput(value);
    if (value.trim().length > 0) {
      const lowerValue = value.toLowerCase();
      // Merge API tags with current tags so newly-added tags also appear as suggestions
      const candidateTags = Array.from(new Set([...allTags, ...tags]));
      const filtered = candidateTags.filter(
        t => t.toLowerCase().includes(lowerValue)
      );
      setSuggestions(filtered.slice(0, 8));
      setShowSuggestions(filtered.length > 0);
    } else {
      setShowSuggestions(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addTag(input);
    }
  };

  return (
    <div ref={wrapperRef} className="relative">
      <label className="block text-sm font-medium text-gray-700 mb-2">Tags</label>
      <div className="flex flex-wrap gap-2 mb-3">
        {tags.map(tag => (
          <span
            key={tag}
            className="bg-primary/10 text-primary px-3 py-1 rounded-full text-sm flex items-center gap-2"
          >
            #{tag}
            <button onClick={() => removeTag(tag)} className="hover:text-primary/70">
              <i className="fas fa-times"></i>
            </button>
          </span>
        ))}
      </div>
      <div className="flex gap-2">
        <input
          type="text"
          placeholder="Type a tag..."
          value={input}
          onChange={(e) => handleInputChange(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => {
            if (input.trim().length > 0 && suggestions.length > 0) {
              setShowSuggestions(true);
            }
          }}
          className="px-4 py-2 border border-gray-300 rounded-lg flex-1"
        />
        <button
          type="button"
          onClick={() => addTag(input)}
          disabled={!input.trim()}
          className="px-4 py-2 bg-primary text-white rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-primary/90 transition-colors"
        >
          <i className="fas fa-plus"></i>
        </button>
      </div>
      {showSuggestions && suggestions.length > 0 && (
        <div className="absolute z-10 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto">
          {suggestions.map(tag => (
            <button
              key={tag}
              type="button"
              onClick={() => addTag(tag)}
              className="w-full text-left px-4 py-2 hover:bg-gray-100 text-sm flex items-center gap-2"
            >
              <i className="fas fa-tag text-gray-400"></i>
              <span>#{tag}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
